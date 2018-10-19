import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;

// How to handle calls to the /data endpoint
class WebAPI implements HttpHandler {
    // List of currently logged in users and their socket addresses
    private HashMap<String, User> userAuthTokens;
    private User currentUser;

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int GOOD = 200;
    private static final int UNAUTHORIZED = 401;
    private static final int BAD_REQUEST = 400;
    private static final int INTERNAL_SERVER_ERROR = 500;


    WebAPI() {
        userAuthTokens = new HashMap<>();
    }

    public void handle(HttpExchange t) throws IOException {

        System.out.println("New Connection");

        Headers headers = t.getResponseHeaders();

        // Necessary for JSONP
        headers.set("Access-Control-Allow-Origin", "*");

        URI uri = t.getRequestURI();
        QueryValues query = new QueryValues(uri.getQuery());

        String callback = query.get("callback");
        if (callback == null) {
            noCallback(t);
            return;
        }

        authenticate(query);

        if(currentUser != null)
            System.out.println("User: " + currentUser.userName);
        else
            System.out.println("User not logged in");

        JSONArray json;

        // Process Query into JSON
        try {
            json = commandRedirect(query);
        } catch (BadQueryException e) {
            exceptionHandler(t, callback, e.getMessage(), BAD_REQUEST);
            return;
        } catch (UnauthenticatedException e) {
            exceptionHandler(t, callback, e.getMessage(), UNAUTHORIZED);
            return;
        } catch (ServerErrorException e){
            exceptionHandler(t,callback, e.getMessage(), INTERNAL_SERVER_ERROR);
            return;
        } catch (Exception e) {
            exceptionHandler(t,callback, "Unknown Error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
            return;
        }

        // Format JSON into http response
        byte[] response = getJSONPMessage(json, callback).getBytes();
        t.sendResponseHeaders(GOOD, response.length);

        try (OutputStream os = t.getResponseBody()) {
            os.write(response);
        }

        t.close();
    }

    // Redirects queries
    private JSONArray commandRedirect(QueryValues query) throws Exception {

        System.out.println("Query keys:" + query.keySet());

        //TODO Implement more query commands

        if (query.containsKey("get"))
            return get(query);

        else if (query.containsKey("signup"))
            return signUp(query);

        else if (query.containsKey("login"))
            return logIn(query);

        else if (query.containsKey("import"))
            return importQuery(query);

        else if(query.containsKey("playlist"))
            return playlist(query);

        else if(query.containsKey("export"))
            return exportQuery(query);

        else if(query.containsKey("search"))
            return search(query);

        else if (query.containsKey("test"))
            return test(query);


        throw new BadQueryException("Query has no meaning");
    }

    // ----------------------------------------  Query Commands  ------------------------------------------------------

    @SuppressWarnings("unchecked")
    private JSONArray search(QueryValues query) {

        //TODO Update with implementation of Spotify.findsong

        Song song = new Song();

        song.title = query.get("search");

        Spotify spotify = new Spotify();
        Song songString = spotify.findSong(song);

        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", songString);
        jsonArray.add(jsonObject);

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    private JSONArray importQuery(QueryValues query) throws Exception {
        if(currentUser == null || currentUser.tokens == null){
            throw new UnauthenticatedException("User needs to log in to service");
        }

        if(!query.containsKey("playlist")){
            return get(query);
        }

        JSONArray jsonArray = new JSONArray();

        Spotify spotify = new Spotify();
        Playlist playlist = spotify.importPlaylist(currentUser.tokens, query.get("playlist"));

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", playlist.Name);
        jsonObject.put("id", playlist.ID);
        jsonArray.add(jsonObject);

        return jsonArray;
    }

    private JSONArray exportQuery(QueryValues query) throws Exception{
        if(currentUser == null || currentUser.tokens == null){
            throw new UnauthenticatedException("User needs to log in to service");
        }
        JSONArray jsonArray = new JSONArray();

        String pid = query.get("export");
        int id = Integer.parseInt(pid);

        Playlist playlist = currentUser.getPlaylistById(id);

        if(playlist == null){
            throw new ServerErrorException("Playlist not found");
        }

        Spotify spotify = new Spotify();
        spotify.exportPlaylist(currentUser.tokens, playlist); //TODO return failed songs

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

        User user = User.CreateUser(info[1], info[2], info[0]);

        json.put("result", user != null);

        jsonArray.add(json);

        return jsonArray;
    }

    // Method to handle "login" query. Returns json with true on success and false on failure
    @SuppressWarnings("unchecked")
    private JSONArray logIn(QueryValues query) throws Exception {
        JSONArray jsonArray = new JSONArray();
        JSONObject json = new JSONObject();

        String encodedName = query.get("login");

        // Expected format is "username:password"
        String[] info = encodedName.split(":", 2);

        boolean b;
        try {
            b = UserPassword.IsPasswordCorrect(info[0], info[1]);
            if (b) {
                User user = User.getUserByUserName(info[0]);

                if (user != null) {
                    String authString = generateAuthToken();
                    userAuthTokens.put(authString, user);
                    json.put("authenticate", authString);
                }
                else
                    b = false;
            }
        } catch (Exception e){
            throw new UnauthenticatedException(e.getMessage());
        }

        json.put("result", b);
        jsonArray.add(json);
        return jsonArray;
    }

    // Method to handle "get" query. Returns json with a list of playlist objects for current user
    @SuppressWarnings("unchecked")
    private JSONArray get(QueryValues query) {
        JSONArray jsonArray = new JSONArray();

        currentUser.FetchPlaylists();
        List<Playlist> playlists = currentUser.playlistList;

        for(Playlist playlist : playlists){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", playlist.Name);
            jsonObject.put("id", playlist.ID);
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    private JSONArray playlist(QueryValues query) throws Exception {
        JSONArray jsonArray = new JSONArray();
        String pid = query.get("playlist");
        int id = Integer.parseInt(pid);

        currentUser.FetchPlaylists();

        Playlist playlist = null;

        for(Playlist p : currentUser.playlistList){
            if(id == p.ID)
                playlist = p;
        }

        if(playlist == null){
            throw new ServerErrorException("Playlist ID not found");
        }

        for(Song song : playlist.getArrayList()){
            JSONObject json = new JSONObject();
            json.put("title", song.title);
            json.put("artist", song.artist);
            json.put("id", song.ID);
            json.put("explicit", song.explicit);
            json.put("album", song.album);
            json.put("duration", song.duration);
            jsonArray.add(json);
        }

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
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
            jsonArray.add(jsonObject);
        }

        return jsonArray;
    }


    // ----------------------------------------  Authentication  ------------------------------------------------------

    private void authenticate(QueryValues query) {
        String authToken = query.get("authenticate");
        if(authToken == null){
            currentUser = null;
            return;
        }
        currentUser = userAuthTokens.get(authToken);
    }

    private String generateAuthToken() {
        String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < 10; i++){
            stringBuilder.append(s.charAt(random.nextInt(s.length())));
        }

        System.out.println("Created token: " + stringBuilder.toString());
        return stringBuilder.toString();
    }


    // ------------------------------------------  Responses  ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void exceptionHandler(HttpExchange t, String callback, String message, int code) throws IOException {
        System.out.println(message);
        JSONObject obj = new JSONObject();
        obj.put("error", message);

        byte[] responseMessage = getJSONPMessage(obj, callback).getBytes();
        t.sendResponseHeaders(code, responseMessage.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(responseMessage);
        }
    }

    private void noCallback(HttpExchange t) throws IOException {
        String msg = "Need callback in query";
        System.out.println("Bad query: " + msg);

        byte[] message = msg.getBytes();
        t.sendResponseHeaders(UNPROCESSABLE_ENTITY, message.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(message);
        }
    }

    private static String getJSONPMessage(Object j, String callback) {
        StringBuilder message = new StringBuilder();
        message.append(callback);
        message.append("(");

        message.append(j.toString());

        System.out.println("Response json:" + j.toString());

        message.append(")");

        return message.toString();
    }

}

// --------------------------------------------  Exceptions  ------------------------------------------------------

class BadQueryException extends Exception{
    BadQueryException(String message) {
        super("Bad Query: " + message);
    }
}

class UnauthenticatedException extends Exception{
    UnauthenticatedException(String message) {
        super("Unauthenticated: " + message);
    }
}

class ServerErrorException extends Exception{
    ServerErrorException(String message) {
        super("Server Error: " + message);
    }
}