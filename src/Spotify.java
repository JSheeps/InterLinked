import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.exceptions.detailed.BadRequestException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.model_objects.specification.User;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import com.wrapper.spotify.requests.data.playlists.AddTracksToPlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.CreatePlaylistRequest;
import com.wrapper.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest;
import com.wrapper.spotify.requests.data.search.simplified.SearchTracksRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import javafx.util.Pair;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class Spotify extends StreamingService
{
    private static final String client_ID = "150469a5b0d949a9b3693a73edc3b46d";
    private static final String client_Secret = "69e5123c458b43fc94d5d380281aee15";
    private static final String scopes = "user-read-birthdate,user-read-email,playlist-modify-private,playlist-read-collaborative,playlist-read-private";

    private static final URI redirectURI = SpotifyHttpManager.makeUri("http://localhost:15000");//temporary redirect uri until this functionality is implemented

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

    //Code retrieved from user authorization is passed, return value is a pair <accessToken, refreshToken>
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
            spotifyApi.setAccessToken(tokens.getKey());
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();
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
            GetCurrentUsersProfileRequest getUserID = spotifyApi.getCurrentUsersProfile().build();
            User user = getUserID.execute();
            String userID = user.getId();
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();
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

                        Song s = new Song(thisSong.getName());
                        s.origin = Song.OriginHostName.SPOTIFY;
                        s.setAlbum(thisSong.getAlbum().getName());
                        s.setArtist(thisSong.getArtists()[0].getName());
                        s.explicit = thisSong.getIsExplicit();
                        s.spotifyURI = thisSong.getUri();
                        s.spotfyID = thisSong.getId();

                        return_list.addSong(s);
                    }
                }
            }
        }catch (Exception e){

        }
        return return_list;
    }

    //tokens are <accessToken, refreshToken>
    public Playlist[] importAllPlaylists(Pair<String, String> tokens)
    {
        try {
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
            Playlist[] return_lists = new Playlist [playlists.getTotal()];
            for (int i = 0; i<return_lists.length;i++){
                return_lists[i] = new Playlist();
                System.out.println(playlists.getItems()[i].getName());
                return_lists[i].setName((playlists.getItems())[i].getName());
            }
            for (int i=0; i<return_lists.length;i++){
                GetPlaylistsTracksRequest getPlaylistTracks = spotifyApi.getPlaylistsTracks(userID,(playlists.getItems())[i].getId())
                        .limit(20)
                        .offset(0)
                        .build();
                final Paging<PlaylistTrack> tracks = getPlaylistTracks.execute();
                //PlaylistTrack [] songs = (tracks.getItems()).clone();
                System.out.println(tracks.getTotal());

                for (int j=0;j<tracks.getTotal();j++){

                    Track thisSong = (tracks.getItems())[j].getTrack();

                    Song s = new Song(thisSong.getName());
                    s.origin = Song.OriginHostName.SPOTIFY;
                    s.setAlbum(thisSong.getAlbum().getName());
                    s.setArtist(thisSong.getArtists()[0].getName());
                    s.explicit = thisSong.getIsExplicit();
                    s.spotifyURI = thisSong.getUri();
                    s.spotfyID = thisSong.getId();
                    return_lists[i].addSong(s);

                }
            }
            return return_lists;

        } catch (Exception e) {e.printStackTrace();}
        return new Playlist[] {};
    }

    public void exportPlaylist(Pair<String,String> tokens,Playlist playlist) {
        List<String> uris = new ArrayList<>();

        //
        for (int i = 0; i < playlist.getNumSongs(); i++) {
            if (playlist.getSong(i).origin == Song.OriginHostName.SPOTIFY) {
                uris.add(playlist.getSong(i).spotifyURI);
            } else {
                //TODO write this function
            }
        }
        try {
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


    public String search(String query){
        SearchTracksRequest searchTracksRequest = spotifyApi.searchTracks(query)
                .limit(10)
                .offset(0)
                .build();
        return "";
    }

    //attempts to match the provided song object with the correct Spotify URI based on given information
    public Song findSong(String query) {
        Song s = new Song();
        try{
            //If the URI is known then the song does not need to be matched
            Track match = new Track.Builder().build();
            SearchTracksRequest searchRequest = spotifyApi.searchTracks(s.getTitle())
                    .limit(10)
                .offset(0)
                        .build();
                Paging<Track> results = searchRequest.execute();
                //int TEMPINTNAMECHANGETHISLATER = closestMatch(s,results);
                match = results.getItems()[0];
                s.setAlbum(match.getAlbum().getName());
            s = trackToSong(match);

        } catch (Exception e){
            e.printStackTrace();
        }
        return s;
    }

    private int closestMatch(Song s, Paging<Track> results){
        return 0;
    }

    private static Song trackToSong(Track t){
        Song s  = new Song();
        s.setAlbum(t.getAlbum().getName());
        s.setDuration(t.getDurationMs());
        s.setExplicit(t.getIsExplicit());
        s.spotifyURI=t.getUri();
        s.origin = Song.OriginHostName.SPOTIFY;

        return s;
    }

    public static String listenToSong(Song s){
        return "";
    }
    //may be used if initial search results are unsatisfactory
    public String search_with_offset(String query, int offset){return "";}
}
