import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executor;

public class JSONPServer {
    public static void main(String[] args) throws Exception {
        DataHandler.data = new Data();

        HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);
        server.createContext("/data", new DataHandler());
        server.setExecutor(new HttpThreadCreator());
        server.start();
    }
}

// Process the HTTP requests on a new thread
class HttpThreadCreator implements Executor {
    @Override public void execute(Runnable command) { new Thread(command).start(); }
}

// How to handle calls to the /data endpoint
class DataHandler implements HttpHandler {
    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int GOOD = 200;

    public static Data data;

    public void handle(HttpExchange t) throws IOException {
        // System.out.println(t.getRequestURI());

        Headers h = t.getResponseHeaders();
        // Necessary for JSONP
        h.set("Access-Control-Allow-Origin", "*");

        URI uri = t.getRequestURI();
        QueryValues query = new QueryValues(uri.getQuery());

        String callback = query.get("callback");
        if (callback == null) {
            badQuery(t, "Need callback in query");
            return;
        }

        JSONObject ret;
        try {
            ret = getStuff(query);
        } catch (Exception e) {
            badQuery(t, e.getMessage());
            return;
        }

        String response = getJSONPmessage(ret, callback);
        t.sendResponseHeaders(GOOD, response.length());

        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes());
        }
        t.close();
    }

    // Expand this for queries
    JSONObject getStuff(QueryValues query) throws Exception {
        if (query.containsKey("list"))
            return listQuery(query);
        else
            throw new Exception("Query has no meaning");
    }

    @SuppressWarnings("all")
    JSONObject listQuery(QueryValues query) throws Exception {
        int listIndex;
        try {
            listIndex = Integer.decode(query.get("list"));
        } catch (NumberFormatException e) {
            throw new Exception("list takes an integer");
        }

        HashMap list;
        try {
            list = data.getList(listIndex);
        } catch (IllegalArgumentException e) {
            throw new Exception("list index out of range");
        }

        JSONObject ret = new JSONObject();
        if (query.containsKey("value")) {
            String valueString = query.get("value");
            if (list.containsKey(valueString)) {
                ret.put("value", list.get(valueString));
            } else
                throw new Exception("value not found");
        } else {
            Iterator it = list.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                ret.put(e.getKey(), e.getValue());
            }
        }

        return ret;
    }

    void badQuery(HttpExchange t, String msg) throws IOException {
        System.out.println("Bad query: " + msg);
        t.sendResponseHeaders(UNPROCESSABLE_ENTITY, msg.length());
        try (OutputStream os = t.getResponseBody()) {
            os.write(msg.getBytes());
        }
    }

    static String getJSONPmessage(JSONObject j, String callback) {
        StringBuilder sb = new StringBuilder();

        sb.append(callback).append('(');
        sb.append(j.toJSONString());
        sb.append(')');

        return sb.toString();
    }
}


// stub class that servers as Data access
class Data {
    private List<HashMap<String, String>> data;

    Data() {
        data = new ArrayList<>();

        HashMap<String, String> list = new HashMap<>();
        list.put("name", "test List");
        list.put("contents", "various test data");
        list.put("test", "test successful");
        list.put("value", "hello");
        list.put("hello", "hi");
        list.put("hi", "hello");
        data.add(list);

        list = new HashMap<>();
        list.put("name", "second List");
        list.put("contents", "other stuff");
        list.put("pi", "3.1415926");
        list.put("root2", "1.41421");
        data.add(list);
    }

    boolean hasUser(String s) {
        return false;
    }

    HashMap<String, String> getList(int index) {
        if (!validListIndex(index))
            throw new IllegalArgumentException("List index out of range");

        return data.get(index);
    }

    int maxIndex() { return data.size(); }
    boolean validListIndex(int i) { return (i >= 0 && i < maxIndex()); }
}