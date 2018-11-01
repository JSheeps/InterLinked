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
    final private String tokenFilePath = "data/tokens.txt";

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int GOOD = 200;

    WebAPI() {
        userAuthTokens = getAuthTokens();
        debug = new Debug(true, true);
    }

    // ----------------------------------------  Server Handlers  ------------------------------------------------------

    public void handle(HttpExchange t) throws IOException {
        debug.log("------------------ Start API Connection ------------------");

        Headers headers = t.getResponseHeaders();

        // Necessary for JSONP
        headers.set("Access-Control-Allow-Origin", "*");

        URI uri = t.getRequestURI();
        QueryValues query;
        try {
            query = new QueryValues(uri.getQuery());
        } catch(Exception e) {
            noCallback(t);
            return;
        }

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

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void serviceLogIn(HttpExchange t) {
        QueryValues query = new QueryValues(t.getRequestURI().getQuery());

        String platformID = query.get("platformID");

        // Code sent from Spotify, used to get user's tokens
        String code = query.get("code");

        // State contains user's interlinked auth token
        String authToken = query.get("state");

        debug.log("~~~User Logged In To: " + platformID);
        debug.log("~~~Code: " + code);
        debug.log("~~~State: " + authToken);

        // Get user associated with interlinked auth token
        User user = userAuthTokens.get(authToken);

        debug.log("~~~Associated user: " + user.userName);

        // Attempt to login using provided code
        try {
            user.tokens = Spotify.Login(code);
        } catch (Exception e) {
            debug.log("~~~Login failed: " + e.getMessage());
            debug.printStackTrace(e);
            return;
        }

        debug.log("~~~Set user's tokens: " + user.tokens.toString());

        debug.log("~~~Finished Login");
    }   

    private Object commandRedirect(QueryValues query) throws Exception {
        // Debug Output
        debug.logVerbose("Connection sent keys{");
        query.keySet().forEach(s -> debug.logVerbose("\t" + s + ", " + query.get(s)));
        debug.logVerbose("}");

        if (query.containsKey("get"))
            return get();

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

        else if(query.containsKey("remove"))
            return remove(query);

        else if(query.containsKey("share"))
            return share(query);

        else if(query.containsKey("importshare"))
            return importShare(query);

        else if(query.containsKey("merge"))
            return merge(query);

        throw new BadQueryException("Query has no meaning");
    }


    // ----------------------------------------  Query Commands  ------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Object search(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        String search = query.get("search");

        // Search for song on Spotify
        Song song;
        try {
            song = Spotify.findSong(search);
        } catch (Exception e) {
            debug.printStackTrace(e);
            throw new ServerErrorException(e.getMessage());
        }

        // Build Json response

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        jsonObject.put("title", song.getTitle());
        jsonObject.put("artist", song.getArtist());
        jsonObject.put("SpotifyURL", Spotify.listenToSong(song));
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

        // Get playlists from spotify
        ArrayList<Playlist> playlists = Spotify.getPlaylists(currentUser.tokens);

        // If not importing a specific playlist, return a list of possible playlists to import
        if(!query.containsKey("playlist")){
            // Get list of importable playlists
            for(Playlist playlist : playlists){
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", playlist.Name);
                jsonArray.put(jsonObject);
            }

            return jsonArray;
        }

        // Find playlist to import
        Playlist playlist = null;
        for(Playlist p : playlists){
            if(p.Name.equals(query.get("playlist")))
                playlist = p;
        }

        if(playlist == null) throw new ServerErrorException("Playlist " + query.get("Playlist") + " not found");

        // Check if playlist already exists
        currentUser.FetchPlaylists();
        if(currentUser.playlistList.contains(playlist) && !query.containsKey("force"))
            throw new ServerErrorException("Playlist already exists in database " +
                    "(to import anyway, send query: force)");

        // Get songs for selected playlist
        List<Song> importPlaylist = Spotify.importPlaylist(currentUser.tokens, playlist.Name);

        // Add songs to new playlist
        playlist.clearSongs();
        debug.log("Found songs:");
        for(Song song : importPlaylist){
            debug.log(song.toString());
            playlist.addSong(song);
            song.save();
        }

        playlist.save(currentUser);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", playlist.Name);
        jsonObject.put("id", playlist.ID);
        jsonArray.put(jsonObject);

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    private Object exportQuery(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");
        if(currentUser.tokens == null)
            throw new NotLoggedInToService("User needs to log in to streaming service");

        String pid = query.get("export");
        int id = Integer.parseInt(pid);

        Playlist playlist = currentUser.getPlaylistById(id);

        if(playlist == null){
            throw new ServerErrorException("Playlist not found");
        }

        ArrayList<String> failedSongs;
        try {
            failedSongs = Spotify.exportPlaylist(currentUser.tokens, playlist);
        } catch (Exception e){
            throw new ServerErrorException(e.getMessage());
        }

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result", failedSongs.size() == 0);
        JSONObject jsonSongs = new JSONObject();
        jsonSongs.put("songs", failedSongs);

        return jsonResult;
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
    private JSONArray get() throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        JSONArray jsonArray = new JSONArray();

        currentUser.FetchPlaylists();

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

        JSONArray jsonArray = new JSONArray();
        String pid = query.get("playlist");
        int id = Integer.parseInt(pid);

        currentUser.FetchPlaylists();

        Playlist playlist = currentUser.getPlaylistById(id);

        if(playlist == null)
            throw new ServerErrorException("Playlist ID not found");

        for(Song song : playlist.FetchSongs()){
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

    @SuppressWarnings("unchecked")
    private Object remove(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        int removeId;
        try {
            removeId = Integer.parseInt(query.get("remove"));
        }
        catch (Exception e){
            throw new BadQueryException("Expected type of remove key is int");
        }

        Playlist playlistById = currentUser.getPlaylistById(removeId);

        if(playlistById == null){
            throw new ServerErrorException("Couldn't find playlist with id: " + removeId);
        }
        else {
            boolean delete = playlistById.delete();
            if(!delete)
                throw new ServerErrorException("Error deleting playlist");
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result",true);

        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    private Object share(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        int shareId;
        try {
            shareId = Integer.parseInt(query.get("share"));
        }
        catch (Exception e){
            throw new BadQueryException("Expected type of share key is int");
        }

        Playlist playlistById = currentUser.getPlaylistById(shareId);

        String shareToken;
        if(playlistById == null){
            throw new ServerErrorException("Couldn't find playlist with id: " + shareId);
        }
        else {
            shareToken = playlistById.generateShareToken();
            if(shareToken == null)
                throw new ServerErrorException("Error generating share token");
        }

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result",true);
        jsonResult.put("share", shareToken);


        return jsonResult;
    }

    @SuppressWarnings("unchecked")
    private Object importShare(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        String shareToken = query.get("importshare");

        boolean b = Playlist.generateSharedPlaylist(shareToken, currentUser);

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result", b);

        return jsonResult;
    }

    @SuppressWarnings("unchecked")
    private Object merge(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        int[] mergeIds;

        String name = query.get("name");
        if (name == null)
            throw new BadQueryException("Must provide a merge list name");

        try {
            String mergeIdString = query.get("merge");
            String[] mergeIdStrings = mergeIdString.split(", ");
            mergeIds = new int[mergeIdStrings.length];

            for (int i = 0; i < mergeIds.length; i++) {
                mergeIds[i] = Integer.parseInt((mergeIdStrings[i]));
            }
        }
        catch (Exception e) {throw new BadQueryException("Unable to parse ids for merge");}

        if (mergeIds.length < 2)
            throw new BadQueryException("Need 2 or more playlists for merging");

        Playlist[] playlists= new Playlist[mergeIds.length];
        for (int i = 0; i < playlists.length; i++) {
            playlists[i] = Playlist.getPlaylistById(mergeIds[i]);
            assert playlists[i] != null;
            playlists[i].setPlaylist(playlists[i].FetchSongs());
            if (playlists[i] == null)
                throw new ServerErrorException("Unable to find playlist: " +
                        mergeIds[i]);
        }

        assert playlists.length >= 2 : "This should be true because we check if there are 2 or more mergeIDs";

        Playlist merge =  playlists[0].merge(playlists[1]);
        for (int i = 2; i < playlists.length; i++)
            merge = merge.merge(playlists[i]);

        merge.Name = name;

        merge.save(currentUser);

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("merge", merge.ID);

        return jsonResult;
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
