import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.wrapper.spotify.model_objects.special.PlaylistTrackPosition;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
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
    public void execute(@NotNull Runnable command) {
        new Thread(command).start();
    }
}

// How to handle calls to the /data endpoint
class DataHandler implements HttpHandler {

    // List of currently logged in users and their socket addresses
    private HashMap<InetSocketAddress, User> activeUsers;

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int GOOD = 200;

    DataHandler() {
        activeUsers = new HashMap<>();
    }

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

        JSONArray json;

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

    // Redirects queries
    private JSONArray commandRedirect(QueryValues query, InetSocketAddress remoteAddress) throws Exception {

        //TODO Implement more query commands

        boolean isLoggedIn = activeUsers.containsKey(remoteAddress);

        if (query.containsKey("get")) {
            if (isLoggedIn)
                return get(activeUsers.get(remoteAddress));
            else
                throw new Exception("User needs to log in");

        } else if (query.containsKey("signup"))
            return signUp(query);

        else if (query.containsKey("login"))
            return logIn(query, remoteAddress);

        else if (query.containsKey("test"))
            return test(query);

        throw new Exception("Query has no meaning");
    }

    private JSONArray test(QueryValues query) {
        JSONArray jsonArray = new JSONArray();

        List<Playlist> list = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < random.nextInt(20); i++) {
            Playlist playlist = new Playlist();
            playlist.Name = "playlist" + i;
            playlist.ID = i;
            list.add(playlist);
        }

        for(Playlist playlist : list){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", playlist.Name);
            jsonObject.put("id", playlist.ID);
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    // Method to handle "signup" query. Returns json with true on success and false on failure
    @SuppressWarnings("unchecked")
    private JSONArray signUp(QueryValues query) {
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject();

        String encodedName = query.get("signup");

        // Format expected is email:username:password
        String[] info = encodedName.split(":", 3);

        User user = User.CreateUser(info[1], info[3], info[0]);

        json.put("result", user != null);

        jsonArray.put(json);

        return jsonArray;
    }

    // Method to handle "login" query. Returns json with true on success and false on failure
    @SuppressWarnings("unchecked")
    private JSONArray logIn(QueryValues query, InetSocketAddress remoteAddress) {
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject();

        String encodedName = query.get("login");

        // Expected format is "username:password"
        String[] info = encodedName.split(":", 2);

        boolean b = false;
        try {
            b = UserPassword.IsPasswordCorrect(info[0], info[1]);
        } catch (Exception ignored){}

        if (b) {
            User user = User.getUserByUserName(info[0]);

            if (user != null)
                activeUsers.put(remoteAddress, user);
            else
                b = false;
        }

        json.put("result", b);
        jsonArray.put(json);
        return jsonArray;
    }

    // Method to handle "get" query. Returns json with a list of playlist objects for current user
    @SuppressWarnings("unchecked")
    private JSONArray get(User user) {
        JSONArray jsonArray = new JSONArray();

        user.FetchPlaylists();
        List<Playlist> playlists = user.playlistList;

        for(Playlist playlist : playlists){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", playlist.Name);
            jsonObject.put("id", playlist.ID);
            jsonArray.put(jsonObject);
        }


        return jsonArray;
    }


    private void badQuery(HttpExchange t, String msg) throws IOException {
        System.out.println("Bad query: " + msg);
        t.sendResponseHeaders(UNPROCESSABLE_ENTITY, msg.length());
        try (OutputStream os = t.getResponseBody()) {
            os.write(msg.getBytes());
        }
    }

    private static String getJSONPMessage(JSONArray j, String callback) {
        StringBuilder message = new StringBuilder();
        message.append(callback);
        message.append("(");

        message.append(j.toString());

        message.append(")");

        System.out.println(message);

        return message.toString();
    }
}