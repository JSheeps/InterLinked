import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.api.PlaylistApi;
import com.github.felixgail.gplaymusic.model.*;
import com.github.felixgail.gplaymusic.model.enums.ResultType;
import com.github.felixgail.gplaymusic.model.requests.SearchTypes;
import com.github.felixgail.gplaymusic.model.responses.SearchResponse;
import com.github.felixgail.gplaymusic.util.TokenProvider;
import com.github.felixgail.gplaymusic.api.GPlayMusic.Builder;
import svarzee.gps.gpsoauth.AuthToken;
import java.util.ArrayList;
import java.util.List;

public class GoogleMusic {
    static GPlayMusic.Builder build = new GPlayMusic.Builder();

    public static String Login(String username, String password, String ANDROID_ID) {
        String auth = "";
        try {

            AuthToken authToken = TokenProvider.provideToken(username, password, ANDROID_ID);
            auth = authToken.getToken();
            build.setAndroidID(ANDROID_ID);
        } catch (java.io.IOException e){e.printStackTrace();}
        catch (svarzee.gps.gpsoauth.Gpsoauth.TokenRequestFailed e){
            e.printStackTrace();
        }
        return auth;
    }

    //returns a list of playlists and their GoogleId's
    public static List<Playlist> getPlaylists(String auth){
        List<Playlist> playlists = new ArrayList<Playlist>();
        GPlayMusic gApi = build.setAuthToken(new AuthToken(auth)).build();
        PlaylistApi plist = gApi.getPlaylistApi();
        try{
            List<com.github.felixgail.gplaymusic.model.Playlist> googleLists = plist.listPlaylists();
            for(int i = 0; i <googleLists.size(); i++){
                Playlist p = new Playlist();
                p.setName(googleLists.get(i).getName());
                p.origin = Origin.GOOGLE;
                p.googleId = googleLists.get(i).getId();
                playlists.add(p);
            }
        } catch (java.io.IOException e){
            e.printStackTrace();
        }
        return playlists;
    }

    public static Playlist importPlaylist(String auth, String id){
        GPlayMusic gApi = build.setAuthToken(new AuthToken(auth)).build();
        PlaylistApi plist = gApi.getPlaylistApi();
        Playlist returnList = new Playlist();
        try{
            com.github.felixgail.gplaymusic.model.Playlist p = plist.getPlaylist(id);
            returnList.setName(p.getName());
            List<PlaylistEntry> googleSongs = p.getContents(100);
            for(int i = 0;i<googleSongs.size(); i++){
                Song s = new Song();
                Track googleS = googleSongs.get(i).getTrack();
                s.setAlbum(googleS.getAlbum());
                s.setTitle(googleS.getTitle());
                s.setArtist(googleS.getArtist());
                s.setOrigin(Origin.GOOGLE);
                s.setGoogleId(googleS.getID());
                returnList.addSong(s);
            }
        } catch (java.io.IOException e){e.printStackTrace();}
        return returnList;
    }

    public static void exportPlaylist(String auth, Playlist playlist){
        if (playlist.getNumSongs() != 0 && playlist != null){
            GPlayMusic gApi = build.setAuthToken(new AuthToken(auth)).build();
            PlaylistApi api = gApi.getPlaylistApi();
            try{
                com.github.felixgail.gplaymusic.model.Playlist google_list = api.create(playlist.getName(),"InterLinked playlist", com.github.felixgail.gplaymusic.model.Playlist.PlaylistShareState.PRIVATE);
                List<String> trackids = new ArrayList<>();
                for (int i=0;i<playlist.getNumSongs(); i++){
                    if (playlist.getSong(i).getOrigin() == Origin.GOOGLE){
                        trackids.add(playlist.getSong(i).googleId);
                    }
                    else{
                        Song s = playlist.getSong(i);
                        String query = s.getTitle() + " " + s.getArtist();
                        trackids.add(getSongId(query));
                    }
                }
                api.addTracksToPlaylistById(google_list,trackids);
            }catch (java.io.IOException e){}
        }
    }

    //Used in export method when only the googleId is needed for a song
    public static String getSongId(String query){
        return findSong(query).googleId;
    }

    public static Song findSong(String query){
        Song s = new Song();
        try {
            SearchResponse response = build.build().search(query,1,new SearchTypes(ResultType.TRACK));
            Track t = response.getTracks().get(0);
            //copy attributes to the Song that will be returned
            s.setTitle(t.getTitle());
            s.setAlbum(t.getAlbum());
            s.setArtist(t.getArtist());
            s.setOrigin(Origin.GOOGLE);
            s.setGoogleId(t.getID());
        }catch (java.io.IOException e){}
        return s;
    }

}
