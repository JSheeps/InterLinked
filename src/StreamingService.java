import javafx.util.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Douglas on 9/22/2018.
 */
public class StreamingService {

    //Call this function to find out the names of playlists on the users account
    //Arguments are an access token and a refresh token
    public static String[] getPlaylistNames(Pair<String, String> tokens){return new String[] {};}

    //Use this function to import a specific Playlist by name
    //Arguments are <Access Token, Refresh Token>, and name of playlist to be imported
    //as returned by getPlaylistNames()
    public static List<Song> importPlaylist(Pair<String,String> tokens, String playlistName){return new ArrayList<>();}

    //
    public static ArrayList<String> exportPlaylist(Pair<String,String> tokens, Playlist playlist){
        return new ArrayList<>();
    }

    public static Song findSong(String query){return new Song();}

    //Argument is the authorization code returned to the redirect URI after the user authorizes us to access their account
    //Returns an access token and a refresh token
   // public static Pair<String, String> Login(String code)
  //  {
  //      return new Pair<String,String>("","");
    //}


    public static String search(String query)
    {
        return "";
    }
}

