import javafx.util.Pair;

/**
 * Created by Douglas on 9/22/2018.
 */
public class StreamingService {

    //Call this function to find out the names of playlists on the users account
    //Arguments are an access token and a refresh token
    public String[] getPlaylistNames(Pair<String, String> tokens){return new String[] {};}

    //Use this function to import a specific Playlist by name
    //Arguments are <Access Token, Refresh Token>, and name of playlist to be imported
    //as returned by getPlaylistNames()
    public Playlist importPlaylist(Pair<String,String> tokens, String playlistName){return new Playlist();}

    //"Legacy Function" that imports all playlists
    public Playlist[] importAllPlaylists(Pair<String, String> tokens)
    {
        return new Playlist [] {};
    }

    //
    public void exportPlaylist(Pair<String,String> tokens, Playlist playlist){

    }

    //Argument is the authorization code returned to the redirect URI after the user authorizes us to access their account
    //Returns an access token and a refresh token
    public Pair<String, String> Login(String code)
    {
        return new Pair<String,String>("","");
    }


    public String search(String query)
    {
        return "";
    }
}

