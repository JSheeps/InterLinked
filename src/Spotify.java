import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.model_objects.specification.User;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.playlists.*;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;
import com.wrapper.spotify.requests.data.tracks.GetTrackRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import org.apache.commons.lang3.tuple.MutablePair;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Spotify extends StreamingService
{
    //SET THIS TO FALSE IF API REQUESTS ARE FAILING
    private static final boolean refreshDebugFlag = true;

    private static final String client_ID = "150469a5b0d949a9b3693a73edc3b46d";
    private static final String client_Secret = "69e5123c458b43fc94d5d380281aee15";
    private static final String scopes = "user-read-birthdate,user-read-email,playlist-modify-private,playlist-read-collaborative,playlist-read-private";

    //This is the URL that the user will be sent to after authorizing us to access their account
    private static final URI redirectURI = SpotifyHttpManager.makeUri("http://localhost/login/?platformID=Spotify");

    private static SpotifyApi.Builder build = new SpotifyApi.Builder()
            .setClientId(client_ID)
            .setClientSecret(client_Secret)
            .setRedirectUri(redirectURI);

    private static MutablePair<String,String> handleTokenRefresh(MutablePair<String, String> tokens){
        if (!refreshDebugFlag) return tokens;

        SpotifyApi s = build.build();
        s.setAccessToken(tokens.getKey());
        s.setRefreshToken(tokens.getValue());
        try {
            AuthorizationCodeCredentials credentials = s.authorizationCodeRefresh().build().execute();
            tokens.setLeft(credentials.getAccessToken());
            return tokens;
        } catch (Exception e){e.printStackTrace();}
        return tokens;
    }

    //URL the user is sent to so they can allow us to access their account
    //State allows the callback to be matched to a user, because state is passed as a parameter with the GET request
    public static String getAuthorizationURL(String state){
        URI temp;
        SpotifyApi spotifyApi = build.build();
        try {
            AuthorizationCodeUriRequest authorizationCodeReq = spotifyApi.authorizationCodeUri().state(state).scope(scopes).show_dialog(true).build();
            temp = authorizationCodeReq.execute();
            return temp.toString();
        } catch (Exception e){e.printStackTrace();}
        return "";
    }

    //Code retrieved at redirect_uri after user authorization has passed, return value is a pair <accessToken, refreshToken>
    public static MutablePair<String,String> Login(String code) throws Exception {
        SpotifyApi spotifyApi = build.build();
        final AuthorizationCodeRequest authorizationcoderequest = spotifyApi.authorizationCode(code).grant_type("authorization_code").build();
        AuthorizationCodeCredentials c = authorizationcoderequest.execute();
        return new MutablePair<>(c.getAccessToken(), c.getRefreshToken());
    }

    public static String[] getPlaylistNames(MutablePair<String, String> tokens){
        List<String> playlist_names = new ArrayList<>();

        tokens = handleTokenRefresh(tokens);
        try {
            SpotifyApi spotifyApi = build.build();
            //The access token must be set to ensure the correct user's playlists are being searched
            spotifyApi.setAccessToken(tokens.getKey());

            //Requesting playlist information from api
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();

            PlaylistSimplified[] items = playlists.getItems();
            for(PlaylistSimplified item : items){
                playlist_names.add(item.getName());
            }

        } catch (Exception e) {e.printStackTrace();}
        return playlist_names.toArray(new String[0]);
    }

    //TODO add an exception for if a playlist hasnt been imported correctly
    public static List<Song> importPlaylist(MutablePair<String, String> tokens, String playlistName){
        handleTokenRefresh(tokens);
        List<Song> return_list = new ArrayList<>();
        try{
            SpotifyApi spotifyApi = build.build();
            spotifyApi.setAccessToken(tokens.getKey());

            //UserID is needed to obtain playlist track information
            GetCurrentUsersProfileRequest getUserID = spotifyApi.getCurrentUsersProfile().build();
            User user = getUserID.execute();
            String userID = user.getId();
            //Requesting users playlist information
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();
            //Searches for the specified playlist for importing
            for (int i=0; i<playlists.getTotal(); i++){
                if ((playlists.getItems()[i].getName()).equals(playlistName)){
                    GetPlaylistsTracksRequest getPlaylistTracks = spotifyApi.getPlaylistsTracks(userID,(playlists.getItems())[i].getId())
                            .limit(100)
                            .offset(0)
                            .build();
                    final Paging<PlaylistTrack> tracks = getPlaylistTracks.execute();

                    for (int j=0;j<tracks.getTotal();j++){

                        Track thisSong = (tracks.getItems())[j].getTrack();
                        Song s = trackToSong(thisSong);
                        return_list.add(s);

                        //check if local playlist has the same number of songs as the Spotify list
                        if (return_list.size() != tracks.getTotal()){

                        }
                    }
                }
            }

        }catch (Exception e){

        }
        return return_list;
    }

    public static List<Song> exportPlaylist(MutablePair<String,String> tokens, Playlist playlist) throws Exception {
        List<String> uris = new ArrayList<>();
        List<Song> failedSongs = new ArrayList<>();
        tokens = handleTokenRefresh(tokens);
        for (Song song : playlist.getArrayList()) {
            if (song.origin == Song.OriginHostName.SPOTIFY && song.getSpotifyURI()!=null) {
                uris.add(song.spotifyURI);
            } else {
                String uri = findURI(song);
                if(uri == null) failedSongs.add(song);
                else uris.add(uri);
            }
        }
        SpotifyApi spotifyApi = build.build();
        if (Arrays.asList(getPlaylistNames(tokens)).contains(playlist.Name)){
            playlist.setName(playlist.Name+"(2)");
        }
        spotifyApi.setAccessToken(tokens.getKey());
        GetCurrentUsersProfileRequest getUserID = spotifyApi.getCurrentUsersProfile().build();
        User user = getUserID.execute();
        String userID = user.getId();
        CreatePlaylistRequest createPlaylist = spotifyApi.createPlaylist(userID, playlist.Name)
                .collaborative(false)
                .public_(false)
                .description("InterLinked custom Playlist")
                .build();
        com.wrapper.spotify.model_objects.specification.Playlist playlist1 = createPlaylist.execute();
        String playlistID = playlist1.getId();
        String [] uri_array = new String [uris.size()];
        uris.toArray(uri_array);
        AddTracksToPlaylistRequest addTracks = spotifyApi.addTracksToPlaylist(userID,playlistID,uri_array)
                .position(0)
                .build();
        addTracks.execute();
        GetPlaylistRequest gpr = spotifyApi.getPlaylist(userID,playlist1.getId()).build();
        com.wrapper.spotify.model_objects.specification.Playlist temp = gpr.execute();
        if(temp.getTracks().getTotal() != playlist.getSize()){
            Paging<PlaylistTrack> success_list = temp.getTracks();
            for (int j=0; j<success_list.getTotal(); j++){
                if (uris.contains(success_list.getItems()[j].getTrack().getUri())){

                }
            }
        }
        return failedSongs;
    }


    private static String findURI(Song s) throws Exception {
        SpotifyApi spotifyApi = build.build();
        ClientCredentialsRequest ccr = spotifyApi.clientCredentials().grant_type("client_credentials").build();
        ClientCredentials cc = ccr.execute();
        spotifyApi.setAccessToken(cc.getAccessToken());
        SearchTracksRequest search = spotifyApi.searchTracks(s.getTitle()+" " + s.getArtist()+ " " + s.getAlbum()).build();
        Paging<Track> tracks = search.execute();
        if(tracks.getItems().length == 0) return null;
        else return tracks.getItems()[0].getUri();
    }

    //Returns the top result from the query as a song object
    public static Song findSong(String query) throws Exception{
        ClientCredentials cc;
        ClientCredentialsRequest ccr;
        SpotifyApi spotifyApi = build.build();

        ccr = spotifyApi.clientCredentials().build();
        cc = ccr.execute();
        spotifyApi.setAccessToken(cc.getAccessToken());
        Track match;
        SearchTracksRequest searchRequest = spotifyApi.searchTracks(query)
                .limit(10)
                .offset(0)
                .build();
        Paging<Track> results = searchRequest.execute();
        Track[] resultSongs = results.getItems();

        if (resultSongs.length == 0) return null;

        match = resultSongs[0];

        return trackToSong(match);
    }

    //Helper function for making a Song object from the Spotify wrappers Track object
    private static Song trackToSong(Track t){
        Song s  = new Song();
        s.setArtist(t.getArtists()[0].getName());
        s.setAlbum(t.getAlbum().getName());
        s.setDuration(t.getDurationMs());
        s.setExplicit(t.getIsExplicit());
        s.spotifyURI = t.getUri();
        s.spotifyID = t.getId();
        s.origin = Song.OriginHostName.SPOTIFY;
        s.setTitle(t.getName());
        return s;
    }

    //returns a url that can be used to open the specified song in Spotify
    public static String listenToSong(Song s) {
        SpotifyApi spotifyApi = build.build();
        ClientCredentialsRequest cc = spotifyApi.clientCredentials().grant_type("client_credentials").build();
        try {
            ClientCredentials tokens = cc.execute();
            Track t;
            spotifyApi.setAccessToken(tokens.getAccessToken());
            if (s.spotifyURI!=null) {
                t = spotifyApi.getTrack(s.spotifyID).build().execute();
                return t.getExternalUrls().get("spotify");
            }
            else {
                //
            }
        } catch (Exception e){e.printStackTrace();}
        return "";
    }

    // Added this to get a list of playlists and their spotify id's
    public static ArrayList<Playlist> getPlaylists(MutablePair<String, String> tokens){
        ArrayList<Playlist> userPlaylists = new ArrayList<>();
        tokens = handleTokenRefresh(tokens);
        try {
            SpotifyApi spotifyApi = build.build();
            //The access token must be set to ensure the correct user's playlists are being searched
            spotifyApi.setAccessToken(tokens.getKey());

            //Requesting playlist information from api
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();

            //Parsing playlist names into a string array
            PlaylistSimplified[] items = playlists.getItems();
            for(PlaylistSimplified item : items){
                Playlist playlist = new Playlist();
                playlist.spotifyId = item.getId();
                playlist.Name = item.getName();
                userPlaylists.add(playlist);
            }

        } catch (Exception e) {e.printStackTrace();}
        return userPlaylists;
    }

    public static Song getSongByID(String id){
        Song song = new Song();
        SpotifyApi s = build.build();
        ClientCredentialsRequest ccr = s.clientCredentials().build();
        try{
            ClientCredentials cc = ccr.execute();
            s.setAccessToken(cc.getAccessToken());
            GetTrackRequest getTrack = s.getTrack(id).build();
            Track t = getTrack.execute();
            song = trackToSong(t);
        } catch (Exception e){

        }

        return song;
    }
}
