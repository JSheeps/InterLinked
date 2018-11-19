import com.github.felixgail.gplaymusic.api.GPlayMusic;
import com.github.felixgail.gplaymusic.api.PlaylistApi;
import com.github.felixgail.gplaymusic.model.PlaylistEntry;
import com.github.felixgail.gplaymusic.model.Track;
import com.github.felixgail.gplaymusic.util.TokenProvider;
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
}
