import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.HashMap;
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

    // List of currently logged in users and their socket addresses
    HashMap<InetSocketAddress, User> activeUsers;

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

        JSONObject json;

        // Process Query into JSON
        try {
            json = commandRedirect(query, t.getRemoteAddress());
        } catch (Exception e) {
            badQuery(t, e.getMessage());
            return;
        }

        // Format JSON into http response
        String response = getJSONPMessage(json, callback);
        t.sendResponseHeaders(GOOD, response.length());

        try (OutputStream os = t.getResponseBody()) {
            os.write(response.getBytes());
        }

        t.close();
    }

    // Expand this for queries
    private JSONObject commandRedirect(QueryValues query, InetSocketAddress remoteAddress) throws Exception {

        //TODO Implement more query commands

        boolean isLoggedIn = activeUsers.containsKey(remoteAddress);

        if (query.containsKey("get")) {

            if(isLoggedIn)
                return get(activeUsers.get(remoteAddress));
            else
                throw new Exception("User needs to log in");

        } else if (query.containsKey("signup"))
            return signUp(query);

        else if (query.containsKey("login"))
            return logIn(query, remoteAddress);

        throw new Exception("Query has no meaning");
    }

    // Method to handle "signup" query. Returns json with true on success and false on failure
    private JSONObject signUp(QueryValues query) {
        JSONObject json = new JSONObject();

        String encodedName = query.get("signup");

        // Format expected is email:username:password
        String[] info = encodedName.split(":", 3);

        User user = User.CreateUser(info[1], info[3], info[0]);

        json.put("result", user != null);

        return json;
    }

    // Method to handle "login" query. Returns json with true on success and false on failure
    private JSONObject logIn(QueryValues query, InetSocketAddress remoteAddress) {
        JSONObject json = new JSONObject();

        String encodedName = query.get("login");

        // Expected format is "username:password"
        String[] info = encodedName.split(":", 2);

        boolean b = UserPassword.IsPasswordCorrect(info[0], info[1]);

        if(b){
            User user = User.getUserByUserName(info[0]);

            if(user != null)
                activeUsers.put(remoteAddress, user);
            else
                b = false;
        }

        json.put("result", b);
        return json;
    }

    // Method to handle "get" query. Returns json with a list of playlist objects for current user
    private JSONObject get(User user) {
        JSONObject json = new JSONObject();

        user.FetchPlaylists();
        List<Playlist> playlists = user.playlistList;

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