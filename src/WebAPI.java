import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

// How to handle calls to the /data endpoint
class WebAPI {
    // List of currently logged in users and their socket addresses
    private HashMap<String, User> userAuthTokens;
    private User currentUser;
    private Debug debug;
    String tokenFilePath = "data/tokens.txt";

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int GOOD = 200;

    WebAPI() {
        userAuthTokens = getAuthTokens();
        debug = new Debug(true, false);
    }

    public void handle(HttpExchange t) throws IOException {
        debug.log("------------------ Start API Connection ------------------");

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
            debug.log("Connected user is logged in. User: " + currentUser.userName);
        else
            debug.log("Connected user is not logged in.");

        Object json;

        // Process Query into JSON
        try {
            json = commandRedirect(query);
        } catch (BadQueryException | NotLoggedInToService | ServerErrorException | UnauthenticatedException e) {
            exceptionHandler(t, callback, e.getMessage());
            t.close();
            debug.printStackTrace(e);
            debug.log("------------------ Connection Completed ------------------");
            return;
        } catch (Exception e) {
            exceptionHandler(t, callback, "Unknown Error: " + e.getMessage());
            t.close();
            debug.printStackTrace(e);
            debug.log("------------------ Connection Completed ------------------");
            return;
        }

        // Format JSON into http response
        byte[] response = getJSONPMessage(json, callback).getBytes();
        t.sendResponseHeaders(GOOD, response.length);

        try (OutputStream os = t.getResponseBody()) {
            os.write(response);
        }

        t.close();

        debug.log("------------------ Connection Completed ------------------");
    }

    public void serviceLogIn(HttpExchange t) {
        QueryValues query = new QueryValues(t.getRequestURI().getQuery());
        String platformID = query.get("platformID");
        String code = query.get("code");
        String authToken = query.get("state");

        debug.log("~~~User Logged In To: " + platformID);
        debug.log("~~~Code: " + code);
        debug.log("~~~State: " + authToken);

        User user = userAuthTokens.get(authToken);

        debug.log("~~~Associated user: " + user.userName);

        Spotify spotify = new Spotify();
        user.tokens = spotify.Login(code);

        debug.log("~~~Set user's tokens: " + user.tokens.toString());

        debug.log("~~~Finished Login");
    }


    // Redirects queries
    private Object commandRedirect(QueryValues query) throws Exception {

        debug.log("Connection sent keys{");
        for(String s : query.keySet()){
            debug.log("\t" + s + ", " + query.get(s));
        }
        debug.log("}");

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

        throw new BadQueryException("Query has no meaning");
    }


    // ----------------------------------------  Query Commands  ------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Object search(QueryValues query) {
        String search = query.get("search");

        Song song = Spotify.findSong(search);

        JSONObject jsonObject = new JSONObject();
        if (song == null) {
            jsonObject.put("error", "Song not found");
            return jsonObject;
        }

        JSONArray jsonArray = new JSONArray();

        jsonObject.put("title", song.getTitle());
        jsonObject.put("artist", song.getArtist());
        jsonObject.put("URL", song.getSpotifyURI());
        jsonArray.put(jsonObject);

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    private JSONArray importQuery(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");
        if(currentUser.tokens == null)
            throw new NotLoggedInToService("User needs to log in to streaming service");

        JSONArray jsonArray = new JSONArray();

        if(!query.containsKey("playlist")){
            // Get list of importable playlists
            ArrayList<Playlist> playlists = Spotify.getPlaylists(currentUser.tokens);
            currentUser.FetchPlaylists();
            for(Playlist spotifyPlaylist : playlists){
                boolean newSpotifyPlaylist = true;
                for(Playlist databasePlaylist : currentUser.playlistList){
                    if(spotifyPlaylist.equals(databasePlaylist)){
                        newSpotifyPlaylist = false;
                    }
                }
                if(newSpotifyPlaylist){
                    currentUser.playlistList.add(spotifyPlaylist);
                }
            }

            for(Playlist playlist : currentUser.playlistList){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", playlist.Name);
                jsonObject.put("id", playlist.ID);
                jsonArray.put(jsonObject);
            }

            return jsonArray;
        }

        // Import a playlist
        int playlistId = Integer.parseInt(query.get("playlist"));
        // TODO: the playlistId is not in the database because it hasn't been imported yet. Give these playlists an ID.
        Playlist playlist = Playlist.getPlaylistById(playlistId);

        Spotify spotify = new Spotify();
        Playlist importPlaylist = spotify.importPlaylist(currentUser.tokens, playlist.Name);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", playlist.Name);
        jsonObject.put("id", playlist.ID);
        jsonArray.put(jsonObject);

        return jsonArray;
    }

    private JSONArray exportQuery(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");
        if(currentUser.tokens == null)
            throw new NotLoggedInToService("User needs to log in to streaming service");

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
    private JSONObject signUp(QueryValues query) {
        JSONObject json = new JSONObject();

        String encodedName = query.get("signup");

        String username = query.get("username");
        String password = query.get("password");
        String email = query.get("email");

        User user = User.CreateUser(username, password, email);

        json.put("result", user != null);

        return json;
    }

    // Method to handle "login" query. Returns json with true on success and false on failure
    @SuppressWarnings("unchecked")
    private JSONObject logIn(QueryValues query) throws Exception {
        JSONObject json = new JSONObject();

        String username = query.get("username");
        String password = query.get("password");

        boolean b;
        try {
            b = UserPassword.IsPasswordCorrect(username, password);
            if (b) {
                User user = User.getUserByUserName(username);

                if (user != null) {
                    String authString = generateAuthToken(user);
                    json.put("authenticate", authString);
                }
                else
                    b = false;
            }
        } catch (Exception e){
            throw new UnauthenticatedException(e.getMessage());
        }

        json.put("result", b);
        return json;
    }

    // Method to handle "get" query. Returns json with a list of playlist objects for current user
    @SuppressWarnings("unchecked")
    private JSONArray get(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");
        if(currentUser.tokens == null)
            throw new NotLoggedInToService("User needs to log in to streaming service");

        JSONArray jsonArray = new JSONArray();


        boolean b = currentUser.FetchPlaylists();
        debug.log("Result of user.FetchPlaylists(): " + b);

        ArrayList<Playlist> playlists = currentUser.playlistList;

        for(Playlist playlist : playlists){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", playlist.Name);
            jsonObject.put("id", playlist.ID);
            jsonArray.put(jsonObject);
        }

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    private JSONArray playlist(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");
        if(currentUser.tokens == null)
            throw new NotLoggedInToService("User needs to log in to streaming service");

        JSONArray jsonArray = new JSONArray();
        String pid = query.get("playlist");
        int id = Integer.parseInt(pid);

        currentUser.FetchPlaylists();

        Playlist playlist = currentUser.getPlaylistById(id);

        if(playlist == null)
            throw new ServerErrorException("Playlist ID not found");

        for(Song song : playlist.getArrayList()){
            JSONObject json = new JSONObject();
            json.put("title", song.title);
            json.put("artist", song.artist);
            json.put("id", song.ID);
            json.put("explicit", song.explicit);
            json.put("album", song.album);
            json.put("duration", song.duration);
            jsonArray.put(json);
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

    private String generateAuthToken(User user) {
        String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        SecureRandom random = new SecureRandom();
        String token;
         do {
             StringBuilder stringBuilder = new StringBuilder();
             for (int i = 0; i < 10; i++) {
                 stringBuilder.append(s.charAt(random.nextInt(s.length())));
             }

             token = stringBuilder.toString();
         } while(userAuthTokens.containsKey(token));

        debug.log("Created token: " + token);

        userAuthTokens.put(token, user);
        String save = token + "\t" + user.userName + "\n";

        try {
            Files.write(Paths.get(tokenFilePath), save.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return token;
    }

    private HashMap<String, User> getAuthTokens() {
        Scanner scanner = null;
        List<String> strings = null;
        try {
            strings = Files.readAllLines(Paths.get(tokenFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        HashMap<String,User> map = new HashMap<>();

        for(String line : strings) {
            String[] columns = line.split("\\s");
            map.put(columns[0],User.getUserByUserName(columns[1]));
        }

        return map;
    }


    // ------------------------------------------  Responses  ---------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void exceptionHandler(HttpExchange t, String callback, String message) throws IOException {
        debug.log("Exception caught: " + message);
        JSONObject obj = new JSONObject();
        obj.put("error", message);

        byte[] responseMessage = getJSONPMessage(obj, callback).getBytes();

        // With JSONP, ALWAYS respond with 200 unless there is not callback
        t.sendResponseHeaders(GOOD, responseMessage.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(responseMessage);
        }
    }

    private void noCallback(HttpExchange t) throws IOException {
        String msg = "Need callback in query";
        debug.log("Bad query: " + msg);

        byte[] message = msg.getBytes();
        t.sendResponseHeaders(UNPROCESSABLE_ENTITY, message.length);
        try (OutputStream os = t.getResponseBody()) {
            os.write(message);
        }
    }

    private String getJSONPMessage(Object j, String callback) {
        StringBuilder message = new StringBuilder();
        message.append(callback);
        message.append("(");

        message.append(j.toString());

        debug.log("Response json:" + j.toString());

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

class NotLoggedInToService extends Exception{
    NotLoggedInToService(String message) {
        super("NotLoggedInToService: " + message);
    }
}

class ServerErrorException extends Exception{
    ServerErrorException(String message) {
        super("Server Error: " + message);
    }
}
