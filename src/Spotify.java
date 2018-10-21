import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.detailed.BadRequestException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.model_objects.specification.User;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.playlists.AddTracksToPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.CreatePlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import javafx.util.Pair;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Spotify extends StreamingService
{
    private static final String client_ID = "150469a5b0d949a9b3693a73edc3b46d";
    private static final String client_Secret = "69e5123c458b43fc94d5d380281aee15";
    private static final String scopes = "user-read-birthdate,user-read-email,playlist-modify-private,playlist-read-collaborative,playlist-read-private";

    //This is the URL that the user will be sent to after authorizing us to access their account
    private static final URI redirectURI = SpotifyHttpManager.makeUri("http://localhost:15000");

    private static SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(client_ID)
            .setClientSecret(client_Secret)
            .setRedirectUri(redirectURI)
            .build();

    private static final AuthorizationCodeUriRequest aCUR = spotifyApi.authorizationCodeUri().scope(scopes).show_dialog(true).build();

    //URL the user is sent to so they can allow us to access their account
    public String getAuthorizationURL(){
        final URI temp = aCUR.execute();
        return temp.toString();
    }

    //Code retrieved at redirect_uri after user authorization has passed, return value is a pair <accessToken, refreshToken>
    public Pair<String,String> Login(String code) {
        try {

            final AuthorizationCodeRequest authorizationcoderequest = spotifyApi.authorizationCode(code).grant_type("authorization_code").build();
            AuthorizationCodeCredentials c = authorizationcoderequest.execute();

            Pair<String, String> tokens = new Pair<String, String> (c.getAccessToken(),c.getRefreshToken());

            return tokens;
        }
        catch (BadRequestException e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return new Pair<String, String> ("error","error");
    }

    public String[] getPlaylistNames(Pair<String, String> tokens){
        String [] playlist_names = {};
        try {
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
            playlist_names = new String[playlists.getTotal()];
            for (int i=0;i<playlists.getTotal(); i++){
                playlist_names[i] = (playlists.getItems()[i]).getName();
            }

        } catch (Exception e) {e.printStackTrace();}
        return playlist_names;
    }

    public Playlist importPlaylist(Pair<String, String> tokens, String playlistName){
        Playlist return_list = new Playlist();
        try{
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
                            .limit(20)
                            .offset(0)
                            .build();
                    final Paging<PlaylistTrack> tracks = getPlaylistTracks.execute();
                    return_list.setName(playlistName);

                    for (int j=0;j<tracks.getTotal();j++){

                        Track thisSong = (tracks.getItems())[j].getTrack();
                        Song s = trackToSong(thisSong);
                        return_list.addSong(s);

                        //check if local playlist has the same number of songs as the Spotify list
                        if (return_list.getSize() != tracks.getTotal()){
                            //TODO error during import, not all songs were added
                        }

                    }
                }
            }

        }catch (Exception e){

        }
        return return_list;
    }

    public void exportPlaylist(Pair<String,String> tokens,Playlist playlist) {
        List<String> uris = new ArrayList<>();

        //
        for (int i = 0; i < playlist.getNumSongs(); i++) {
            if (playlist.getSong(i).origin == Song.OriginHostName.SPOTIFY) {
                uris.add(playlist.getSong(i).spotifyURI);
            } else {
                uris.add(findURI(playlist.getSong(i)));
            }
        }
        try {
            if (Arrays.asList(getPlaylistNames(tokens)).contains(playlist.Name)){
                //TODO handle duplicate playlist names
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String findURI(Song s) {
        ClientCredentialsRequest ccr = spotifyApi.clientCredentials().grant_type("client_credentials").build();
        try {
            ClientCredentials cc = ccr.execute();

        } catch (Exception e){e.printStackTrace();}
        return "";
    }

    //Returns the top result from the query as a song object
    public Song findSong(String query) {
        Song s = new Song();
        ClientCredentials cc;
        ClientCredentialsRequest ccr;
        try{
            ccr = spotifyApi.clientCredentials().build();
            cc = ccr.execute();
            spotifyApi.setAccessToken(cc.getAccessToken());
            Track match;
            SearchTracksRequest searchRequest = spotifyApi.searchTracks(query)
                    .limit(10)
                    .offset(0)
                    .build();
            Paging<Track> results = searchRequest.execute();
            match = results.getItems()[0];
            s = trackToSong(match);
            System.out.println(s.getTitle());

        } catch (Exception e){
            e.printStackTrace();
        }
        return s;
    }

    private int closestMatch(Song s, Paging<Track> results){
        return 0;
    }

    //Helper function for making a Song object from the Spotify wrappers Track object
    private static Song trackToSong(Track t){
        Song s  = new Song();
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

    //tokens are <accessToken, refreshToken>
    public Playlist[] importAllPlaylists(Pair<String, String> tokens)
    {
        try {
            //Fetch a list of the users playlists
            //User ID is required in playlist requests, so that is obtained first
            spotifyApi.setAccessToken(tokens.getKey());
            GetCurrentUsersProfileRequest getUserID = spotifyApi.getCurrentUsersProfile().build();
            User user = getUserID.execute();
            String userID = user.getId();
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();

            //Create and name playlists in the return array
            Playlist[] return_lists = new Playlist [playlists.getTotal()];
            for (int i = 0; i<return_lists.length;i++){
                return_lists[i] = new Playlist();
                return_lists[i].setName((playlists.getItems())[i].getName());
            }

            //Get the Tracks for each playlist, and add them to the playlist array as songs
            for (int i=0; i<return_lists.length;i++){
                GetPlaylistsTracksRequest getPlaylistTracks = spotifyApi.getPlaylistsTracks(userID,(playlists.getItems())[i].getId())
                        .limit(20)
                        .offset(0)
                        .build();
                final Paging<PlaylistTrack> tracks = getPlaylistTracks.execute();

                for (int j=0;j<tracks.getTotal();j++){
                    Track thisSong = (tracks.getItems())[j].getTrack();
                    return_lists[i].addSong(trackToSong(thisSong));
                }
            }

            return return_lists;
        } catch (Exception e) {e.printStackTrace();}
        return new Playlist[] {};
    }
}
