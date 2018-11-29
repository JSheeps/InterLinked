import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;
import svarzee.gps.gpsoauth.Gpsoauth;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// How to handle calls to the /data endpoint
class WebAPI {
    private UserSessions userAuthTokens;
    private User currentUser;
    private Debug debug;

    // For emails:
    private static Session emailSession;

    static {
        String user = "interlinkednoreply";
        String password = "Interlinked";

        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.debug", true);
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", 587);
        prop.put("mail.smtp.user", user);
        prop.put("mail.smtp.password", password);
        // SSL
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.ssl.enable", false);

        // Authentication for SMTP server
        emailSession = Session.getDefaultInstance(prop
                ,new javax.mail.Authenticator() {
                    protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                        return new javax.mail.PasswordAuthentication(user, password);
                    }
                }
        );
    }

    private static final int UNPROCESSABLE_ENTITY = 422;
    private static final int GOOD = 200;

    WebAPI(UserSessions sessions) {
        this.userAuthTokens = sessions;
        debug = new Debug(true, true);
    }

    // ----------------------------------------  Server Handlers  ------------------------------------------------------

    void handle(HttpExchange t) throws IOException {
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
            exceptionHandler(t, callback, e.getMessage());
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

        // Code sent from Spotify, used to get user's spotifyTokens
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
        Runnable runnable = () -> {
            try {
                if (platformID.equals("Spotify"))
                    user.updateSpotifyToken(Spotify.Login(code));
                else if (platformID.equals("Youtube"))
                    user.updateYoutubeToken(Youtube.GetToken(code));

                debug.log("~~~Set user's tokens.");

                debug.log("~~~Finished Login");

            } catch (Exception e) {
                debug.log("~~~Login failed: " + e.getMessage());
                debug.printStackTrace(e);
            }
        };

        ExecutorService execute = Executors.newCachedThreadPool();
        execute.execute(runnable);
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

        else if (query.containsKey("googlePlayLogin"))
            return googlePlayLogin(query);

        else if (query.containsKey("login"))
            return logIn(query);

        else if (query.containsKey("import"))
            return importQuery(query);

        else if(query.containsKey("removeSong"))
            return removeSong(query);

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

        else if(query.containsKey("add"))
            return addSong(query);

        else if(query.containsKey("revert"))
            return revert(query);

        else if(query.containsKey("forgotPassword"))
            return forgotPassword(query);

        else if(query.containsKey("resetToken"))
            return resetPassword(query);

        else if(query.containsKey("changePassword"))
            return changePassword(query);

        throw new BadQueryException("Query has no meaning");
    }


    // ----------------------------------------  Query Commands  ------------------------------------------------------
    @SuppressWarnings("unchecked")
    private Object search(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        String search = query.get("search");
        String platform = query.get("platformID");
        if (platform == null)
            throw new BadQueryException("No platform provided");

        // Search for song on Spotify
        Song song;

        try {
            switch (platform) {
                case "Spotify":
                    song = Spotify.findSong(search);
                    break;

                case "GooglePlayMusic":
                    song = GoogleMusic.findSong(search);
                    break;

                case "Youtube":
                    song = Youtube.findSong(search);
                    break;

                default:
                    throw new Exception("Unknown platform");
            }
        } catch (Exception e) {
            debug.printStackTrace(e);
            throw new ServerErrorException(e.getMessage());
        }

        if (song == null)
            throw new ServerErrorException("No results");

        // Build Json response

        JSONObject jsonObject = new JSONObject();
        JSONArray jsonArray = new JSONArray();

        jsonObject.put("title", song.getTitle());
        jsonObject.put("artist", song.getArtist());
        jsonObject.put("SpotifyURL", Spotify.listenToSong(song));
        jsonObject.put("SpotifyID", song.spotifyID);
        jsonArray.put(jsonObject);

        return jsonArray;
    }

    @SuppressWarnings("unchecked")
    private JSONArray importQuery(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");
        if(currentUser.spotifyTokens == null && currentUser.youtubeToken == null && currentUser.googleMusicToken == null)
            throw new NotLoggedInToService("User needs to log in to a streaming service");

        JSONArray jsonArray = new JSONArray();

        ArrayList<Playlist> playlists = new ArrayList<>();

        // Get playlists from spotify
        if(currentUser.spotifyTokens != null)
            playlists.addAll(Spotify.getPlaylists(currentUser.spotifyTokens));

        // Get playlists from youtube
        if(currentUser.youtubeToken != null)
            playlists.addAll(Youtube.getPlaylists(currentUser.youtubeToken));

        // Get playlists from google play
        if(currentUser.googleMusicToken != null)
            playlists.addAll(GoogleMusic.getPlaylists(currentUser.googleMusicToken));

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
        Playlist selectedPlaylist = null;
        for(Playlist p : playlists){
            if(p.Name.equals(query.get("playlist")))
                selectedPlaylist = p;
        }

        if(selectedPlaylist == null) throw new ServerErrorException("Playlist " + query.get("Playlist") + " not found");

        Playlist playlistToImport = selectedPlaylist;

        // Check if playlist already exists
        currentUser.FetchPlaylists();
        if(currentUser.playlistList.contains(selectedPlaylist)) {
            if(!query.containsKey("force"))
                throw new ServerErrorException("Playlist already exists in database " +
                        "(to import anyway, send query: force)");
            else
                playlistToImport = currentUser.getPlaylistByName(selectedPlaylist.Name);
        }

        List<Song> songList;
        try {
            // Get songs for selected playlist
            if(selectedPlaylist.origin == Origin.SPOTIFY)
                songList = Spotify.importPlaylist(currentUser.spotifyTokens, selectedPlaylist.Name);
            else if(selectedPlaylist.origin == Origin.YOUTUBE)
                songList = Youtube.importPlaylist(currentUser.youtubeToken, selectedPlaylist.Name);
            else if(selectedPlaylist.origin == Origin.GOOGLE)
                songList = GoogleMusic.importPlaylist(currentUser.googleMusicToken, selectedPlaylist.googleId);
            else
                throw new Exception("Failed to determine playlist origin");

            if(songList.size() == 0)
                throw new Exception("Playlist appears to be empty.");

            // Add songs to new playlist
            playlistToImport.clearSongs();
            debug.log("Found songs:");
            for (Song song : songList) {
                debug.log(song.toString());
                playlistToImport.addSong(song);
                song.save();
            }

            playlistToImport.save(currentUser);

        } catch (Exception e) {throw new ServerErrorException("Error importing playlist: " + e.getMessage());}

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", selectedPlaylist.Name);
        jsonObject.put("id", selectedPlaylist.ID);
        jsonArray.put(jsonObject);

        return jsonArray;
    }

    private Object googlePlayLogin(QueryValues query) throws Exception {
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        String username = query.get("googlePlayLogin");
        String password = query.get("password");
        String imei = query.get("imei");

        String playToken;
        try {
            playToken = GoogleMusic.Login(username, password, imei);
        } catch (Gpsoauth.TokenRequestFailed e) {
            return new JSONObject().put("result", false).put("error", "Invalid authorization details");
        }

        currentUser.updateGoogleMusicToken(playToken);

        return new JSONObject().put("result", true);
    }

    @SuppressWarnings("unchecked")
    private Object exportQuery(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        String exportPlatform = query.get("platformID");

        if(exportPlatform.equals("GooglePlayMusic") && currentUser.googleMusicToken == null)
            throw new NotLoggedInToService("Service login is required for export");
        if(exportPlatform.equals("Spotify") && currentUser.spotifyTokens == null)
            throw new NotLoggedInToService("Service login is required for export");


        String pid = query.get("export");
        int id = Integer.parseInt(pid);

        Playlist playlist = currentUser.getPlaylistById(id);

        if(playlist == null){
            throw new ServerErrorException("Playlist not found");
        }

        List<Song> failedSongs = new ArrayList<>();
        try {
            if(exportPlatform.equals("Spotify"))
                failedSongs = Spotify.exportPlaylist(currentUser.spotifyTokens, playlist);
            else if(exportPlatform.equals("GooglePlayMusic"))
                failedSongs = GoogleMusic.exportPlaylist(currentUser.googleMusicToken, playlist);
        } catch (Exception e){
            throw new ServerErrorException(e.getMessage());
        }

        debug.log("Failed songs:");
        failedSongs.forEach(song -> debug.log("  " + song.toString()));

        JSONObject jsonResult = new JSONObject();
        jsonResult.put("result", true);
        JSONObject jsonSongs = new JSONObject();
        jsonSongs.put("songs", failedSongs);

        if(failedSongs.size() > 0){
            StringBuilder msg = new StringBuilder("" + failedSongs.size() + " failed song(s):\n");

            for(Song song : failedSongs)
                msg.append("    ").append(song.title).append(" - ").append(song.artist).append("\n");

            throw new Exception(msg.toString());
        }

        return jsonResult;
    }

    // Method to handle "signup" query. Returns json with true on success and false on failure
    @SuppressWarnings("unchecked")
    private JSONObject signUp(QueryValues query) throws BadQueryException {
        JSONObject json = new JSONObject();

        String username = query.get("username");
        String password = query.get("password");
        String email = query.get("email");

        if (username == null || username.equals(""))
            throw new BadQueryException("Username is empty");

        if (password == null || password.equals(""))
            throw new BadQueryException("Password is empty");

        if (email == null || email.equals(""))
            throw new BadQueryException("Email is empty");

        User user = User.getUserByUserName(username);
        if (user != null) {
            json.put("result", false);
            json.put("error", "User already exists");
            return json;
        }

        user = User.CreateUser(username, password, email);

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
        } catch (Exception e){
            throw new UnauthenticatedException(e.getMessage());
        }

        if (b) {
            User user = User.getUserByUserName(username);

            if (user != null) {
                String authString = userAuthTokens.generateAuthToken(user);
                json.put("authenticate", authString);
                user.setAuthToken(authString);
            }
            else
                b = false;
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

        currentUser.FetchPlaylists();
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

    private Object removeSong(QueryValues query) throws Exception {
        if (currentUser == null)
            throw new UnauthenticatedException("User needs to log into interlinked");

        int removeIndex;
        try {
            removeIndex = Integer.parseInt(query.get("removeSong"));
        } catch (Exception e) {
            throw new BadQueryException("Expected type of removeSong key is int");
        }

        int playlistID;
        try {
            playlistID = Integer.parseInt(query.get("playlist"));
        } catch (Exception e) {
            throw new BadQueryException("Expected type of removeSong key is int");
        }

        currentUser.FetchPlaylists();

        Playlist playlist = currentUser.getPlaylistById(playlistID);
        if (playlist == null)
            throw new ServerErrorException("Couldn't find playlist with id: " + playlistID);
        else {
            if (playlist.removeSong(removeIndex) == null)
                throw new ServerErrorException("Error deleting song");
            playlist.save(currentUser);
        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", true);

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

    @SuppressWarnings("unchecked")
    private Object addSong(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        String add = query.get("add");
        String[] s = add.split(" ");
        if(s.length != 2){
            throw new BadQueryException("Add expects <playlistid songid>, got: " + add);
        }
        int playlistId = Integer.parseInt(s[0]);
        String songId = s[1];

        Playlist playlist = Playlist.getPlaylistById(playlistId);
        Song song = Spotify.getSongByID(songId);

        if(playlist == null) throw new ServerErrorException("Couldn't find playlist with id: " + playlistId);
        if(song == null) throw new ServerErrorException("Couldn't find song with id: " + songId);

        playlist.setPlaylist(playlist.FetchSongs());
        playlist.addSong(song);

        playlist.save(currentUser);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", true);
        return jsonObject;
    }

    @SuppressWarnings("unchecked")
    private Object revert(QueryValues query) throws Exception{
        if(currentUser == null)
            throw new UnauthenticatedException("User needs to log in to interLinked");

        int playlistId = Integer.parseInt(query.get("revert"));

        Playlist playlist = Playlist.getPlaylistById(playlistId);

        if(playlist == null) throw new ServerErrorException("Couldn't find playlist with id: " + playlistId);

        List<Playlist> playlists = playlist.fetchPreviousStates();

        if(playlists == null) throw new ServerErrorException("Server error");
        if(playlists.size() == 0)
            throw new ServerErrorException("Couldn't find previous states for playlist: " + playlist.Name);

        playlists.get(0).save(currentUser);

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("result", true);
        return jsonObject;
    }

    private Object forgotPassword(QueryValues query) throws Exception {
        JSONObject result = new JSONObject();

        String username = query.get("forgotPassword");
        User user = User.getUserByUserName(username);
        if (user == null) {
            result.put("error", "User does not exist");
            return result;
        }

        String email = user.email;
        String obfuscatedEmail;
        try {
            obfuscatedEmail = obfuscateEmail(email);
        } catch (IllegalArgumentException e) {
            result.put("error", "Invalid email provided at creation.");
            return result;
        }

        try {
            // Send email
            MimeMessage message = new MimeMessage(emailSession);
            message.setFrom(new InternetAddress(Server.domain));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
            message.setSubject("Interlinked - Reset Password");

            StringBuilder contents = new StringBuilder("Hello ");
            contents.append(username).append(",\n");
            String resetToken = userAuthTokens.generateResetToken(user);
            URI redirectURI = new URI("http", Server.domain, "/Login Page/recoverPassword.html", "resetToken=" + resetToken, null);
            contents.append("To reset your password, goto ").append(redirectURI).append('\n');
            contents.append("This link will be valid for 1 hour");
            message.setText(contents.toString());
            Transport.send(message);
        } catch(MessagingException e) {
            e.printStackTrace();
            result.put("error", "Could not send email to " + obfuscatedEmail);
            return result;
        } catch (Exception e) {
            result.put("error", "Could not send email to " + obfuscatedEmail);
            return result;
        }

        result.put("email", obfuscatedEmail);
        return result;
    }

    private Object resetPassword(QueryValues query) throws Exception {
        String resetToken = query.get("resetToken");
        String newPassword = query.get("newPassword");

        if (resetToken == null)
            throw new BadQueryException("Missing reset token");

        if (newPassword == null)
            throw new BadQueryException("Missing new password");

        JSONObject result = new JSONObject();
        User user = userAuthTokens.getUserWithResetToken(resetToken);
        if (user == null) {
            result.put("result", false);
            result.put("error", "No longer able to reset user's password with this link");
            return result;
        }

        if (changePassword(user, newPassword, result))
            userAuthTokens.deleteResetSession(resetToken);

        return result;
    }

    private Object changePassword(QueryValues query) throws Exception {
        String userName = query.get("changePassword");
        String password = query.get("password");
        String newPassword = query.get("newPassword");
        if (userName == null)
            throw new BadQueryException("Missing changePassword value [username]");

        if (password == null)
            throw new BadQueryException("Missing password");

        if (newPassword == null)
            throw new BadQueryException("Missing newPassword");

        JSONObject result = new JSONObject();

        if (!UserPassword.IsPasswordCorrect(userName, password)) {
            result.put("error", "Invalid login details");
            return result;
        }


        User user = User.getUserByUserName(userName);
        changePassword(user, newPassword, result);

        return result;
    }

    private static boolean changePassword(User user, String password, JSONObject result) throws BadQueryException {
        if (user == null)
            throw new BadQueryException("User does not exist");

        if (UserPassword.CreateUserPassword(user.ID, password)) {
            result.put("result", true);
            return true;
        } else {
            result.put("result", false);
            result.put("error", "Bad password");
            return false;
        }
    }

    // "abcde"... "@domain" -> "ab...@domain"
    private static String obfuscateEmail(String email) {
        int atIndex = email.lastIndexOf('@');
        if (atIndex == -1)
            throw new IllegalArgumentException("Invalid email: no @ sign");
        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        StringBuilder obfuscatedEmail = new StringBuilder();
        if (localPart.length() < 2)
            obfuscatedEmail.append(localPart);
        else {
            for (int i = 0; i < 2; i++)
                obfuscatedEmail.append(localPart.charAt(i));
        }
        obfuscatedEmail.append("...");

        obfuscatedEmail.append(domain);
        return obfuscatedEmail.toString();
    }

    // ----------------------------------------  Authentication  ------------------------------------------------------
    private void authenticate(QueryValues query) {
        String authToken = query.get("authenticate");
        if(authToken == null){
            currentUser = null;
            return;
        }
        User user = User.getUserByUserName(query.get("user"));
        if (user == null) {
            currentUser = null;
            return;
        }

        currentUser =  (user.isAuthTokenValid(query.get("authenticate"))) ?
                currentUser = user :
                null;
        // currentUser = userAuthTokens.get(authToken);
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
        super("Not logged in: " + message);
    }
}

class ServerErrorException extends Exception{
    ServerErrorException(String message) {
        super("Server Error: " + message);
    }
}
