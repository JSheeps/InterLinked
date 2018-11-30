import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.api.PlaylistApi;
import com.github.felixgail.gplaymusic.model.PlaylistEntry;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.model.enums.ResultType;
import com.github.felixgail.gplaymusic.model.requests.SearchTypes;
import com.github.felixgail.gplaymusic.model.responses.SearchResponse;
import com.github.felixgail.gplaymusic.util.TokenProvider;
import svarzee.gps.gpsoauth.AuthToken;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoogleMusic {
    static GPlayMusic.Builder build = new GPlayMusic.Builder();

    public static String Login(String username, String password, String ANDROID_ID) throws svarzee.gps.gpsoauth.Gpsoauth.TokenRequestFailed, IOException {
        String auth;
        AuthToken authToken = TokenProvider.provideToken(username, password, ANDROID_ID);
        auth = authToken.getToken();
        build.setAndroidID(ANDROID_ID);
        return auth;
    }

    //returns a list of playlists and their GoogleId's
    public static List<Playlist> getPlaylists(String auth) throws IOException {
        List<Playlist> playlists = new ArrayList<>();
        AuthToken tok = TokenProvider.provideToken(auth);
        GPlayMusic gApi = build.setAuthToken(tok).build();
        PlaylistApi plist = gApi.getPlaylistApi();
        List<com.github.felixgail.gplaymusic.model.Playlist> googleLists = plist.listPlaylists();
        //plist.deletePlaylists(googleLists.get(0)); used this to delete invisible playlist, keep for future reference
        for (com.github.felixgail.gplaymusic.model.Playlist googleList : googleLists) {
            Playlist p = new Playlist();
            p.setName(googleList.getName());
            p.origin = Origin.GOOGLE;
            p.googleId = googleList.getId();
            playlists.add(p);
        }
        return playlists;
    }

    public static List<Song> importPlaylist(String auth, String id) throws Exception {
        AuthToken tok = TokenProvider.provideToken(auth);
        GPlayMusic gApi = build.setAuthToken(tok).build();
        PlaylistApi plist = gApi.getPlaylistApi();
        List<Song> returnList = new ArrayList<>();

        com.github.felixgail.gplaymusic.model.Playlist p = plist.getPlaylist(id);
        List<PlaylistEntry> googleSongs = p.getContents(200);
        for (PlaylistEntry googleSong : googleSongs) {
            Song s = new Song();
            Track googleS = googleSong.getTrack();
            s.setAlbum(googleS.getAlbum());
            s.setTitle(googleS.getTitle());
            s.setArtist(googleS.getArtist());
            s.setOrigin(Origin.GOOGLE);
            s.setGoogleId(googleS.getID());
            returnList.add(s);
        }
        return returnList;
    }

    public static List<Song> exportPlaylist(String auth, Playlist playlist) throws Exception {
        List<Song> failedSongs = new ArrayList<>();
        if (playlist.getNumSongs() != 0){
            AuthToken tok = TokenProvider.provideToken(auth);
            GPlayMusic gApi = build.setAuthToken(tok).build();
            PlaylistApi api = gApi.getPlaylistApi();
            com.github.felixgail.gplaymusic.model.Playlist google_list = api.create(playlist.getName(),"InterLinked playlist", com.github.felixgail.gplaymusic.model.Playlist.PlaylistShareState.PRIVATE);
            List<String> trackids = new ArrayList<>();
            for (int i=0;i<playlist.getNumSongs(); i++){
                if (playlist.getSong(i).getOrigin() == Origin.GOOGLE){
                    trackids.add(playlist.getSong(i).googleId);
                }
                else {
                    Song s = playlist.getSong(i);
                    String query = s.getTitle() + " " + (s.getArtist().equals("unknown")? "" : s.getArtist());
                    System.out.println("Searching for: " + query);
                    try {
                        trackids.add(getSongId(query));
                    }
                    catch (Exception e){
                        failedSongs.add(s);
                    }
                }
            }

            System.out.println("Found songs: " + trackids.size() + " Failed songs: " + failedSongs.size());

            int endIndex = 0;
            for(int i = 0; i < trackids.size() - 20; i += 20){
                List<String> idList = new ArrayList<>(trackids.subList(i, i + 20));
                System.out.println("Exporting tracks: " + i + " to " + (i+20));
                api.addTracksToPlaylistById(google_list, idList);

                endIndex = i + 20;
            }
            System.out.println("Exporting tracks: " + endIndex + " to " + (trackids.size()-1));
            List<String> idList = new ArrayList<>(trackids.subList(endIndex, trackids.size()));
            api.addTracksToPlaylistById(google_list, idList);
        }
        else throw new Exception("Playlist to export is empty");
        return failedSongs;
    }

    //Used in export method when only the googleId is needed for a song
    public static String getSongId(String query) throws Exception {
        return findSong(query).googleId;
    }

    public static Song findSong(String query) throws Exception {
        Song s = new Song();
        SearchResponse response = build.build().search(query,1,new SearchTypes(ResultType.TRACK));
        List<Track> tracks = response.getTracks();
        Track t = null;
        if(tracks.size() > 0)
            t = response.getTracks().get(0);
        if(t == null){
            throw new Exception("Track not found");
        }
        //copy attributes to the Song that will be returned
        s.setTitle(t.getTitle());
        s.setAlbum(t.getAlbum());
        s.setArtist(t.getArtist());
        s.setOrigin(Origin.GOOGLE);
        s.setGoogleId(t.getID());
        return s;
    }

}
