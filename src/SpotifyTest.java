import javafx.scene.effect.Light;
import javafx.util.Pair;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.tuple.MutablePair;

public class SpotifyTest {

    public static void main(String[] args) {
        String authcode =
""
                ;

        String state = "";
        try {
            MutablePair<String, String> tokens;
            System.out.println(Spotify.getAuthorizationURL(state));
            tokens = Spotify.Login(authcode);
            System.out.println("Access: "+tokens.getLeft()+"\nRefresh: "+ tokens.getRight());
            Spotify.getPlaylistNames(tokens);
            System.out.println("Access: "+tokens.getLeft()+"\nRefresh: "+ tokens.getRight());
            Spotify.getPlaylistNames(tokens);
            System.out.println("Access: "+tokens.getLeft()+"\nRefresh: "+ tokens.getRight());
        } catch (Exception e){e.printStackTrace();}
    }
}
