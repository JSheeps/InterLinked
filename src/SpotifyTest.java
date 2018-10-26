import javafx.scene.effect.Light;

public class SpotifyTest {

    public static void main(String[] args) {
        String state = "";
        try {

            System.out.println(Spotify.getAuthorizationURL(state));
            System.out.println("Login+ "+ Spotify.Login(""));
        } catch (Exception e){e.printStackTrace();}
    }
}
