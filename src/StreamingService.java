import javafx.util.Pair;

/**
 * Created by Douglas on 9/22/2018.
 */
public class StreamingService {
    public Playlist importPlaylists()
    {
        return new Playlist();
    }

    public void exportPlaylist(Pair<String,String> tokens, Playlist playlist){

    }

    public Playlist[] getPlaylists(){
        return new Playlist[] {};
    }
    public Pair<String, String> Login()
    {
        return new Pair<String,String>("","");
    }
    public String findSong(Song s)
    {
        return "";
    }
}

