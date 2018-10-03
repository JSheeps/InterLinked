import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class JSONPServer {
    public static void main(String[] args) {
        try {

            HttpServer server = HttpServer.create(new InetSocketAddress(80), 0);

            server.createContext("/data", new DataHandler());

            server.setExecutor(new HttpThreadCreator());

            server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

// Process the HTTP requests on a new thread
class HttpThreadCreator implements Executor {
    @Override
    public void execute(Runnable command) {
        new Thread(command).start();
    }
}

// How to handle calls to the /data endpoint
class DataHandler implements HttpHandler {

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int GOOD = 200;

    public void handle(HttpExchange t) throws IOException {
        Headers headers = t.getResponseHeaders();

        // Necessary for JSONP
        headers.set("Access-Control-Allow-Origin", "*");

        URI uri = t.getRequestURI();
        QueryValues query = new QueryValues(uri.getQuery());

        String callback = query.get("callback");
        if (callback == null) {
            badQuery(t, "Need callback in query");
            return;
        }

        JSONObject jsonReturn;

        // Process Query into JSON
        try {
            jsonReturn = commandRedirect(query);
        } catch (Exception e) {
            badQuery(t, e.getMessage());
            return;
        }

        // Format JSON into http response
        String response = getJSONPMessage(jsonReturn, callback);
        t.sendResponseHeaders(GOOD, response.length());

        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes());
        }

        t.close();
    }

    // Expand this for queries
    private JSONObject commandRedirect(QueryValues query) throws Exception {

        //TODO Implement more query commands

        if (query.containsKey("example")) {
        } else if (query.containsKey("get")) {
            return get(query);
        }

        throw new Exception("Query has no meaning");
    }

    @SuppressWarnings("unchecked")
    private JSONObject get(QueryValues query) {
        JSONObject json = new JSONObject();

        List<String> playlists = new ArrayList<>();

        playlists.add("Example playlist 1");
        playlists.add("Example playlist 2");

        json.put("playlists", playlists);

        return json;
    }


    private void badQuery(HttpExchange t, String msg) throws IOException {
        System.out.println("Bad query: " + msg);
        t.sendResponseHeaders(UNPROCESSABLE_ENTITY, msg.length());
        try (OutputStream os = t.getResponseBody()) {
            os.write(msg.getBytes());
        }
    }

    private static String getJSONPMessage(JSONObject j, String callback) {

        return callback + '(' +
                j.toJSONString() +
                ')';

    }
}