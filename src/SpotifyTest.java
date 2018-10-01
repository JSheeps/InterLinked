import java.util.*;
import java.net.*;
import com.wrapper.spotify.*;

import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
/**
 * Created by Douglas on 9/22/2018.
 */
public class SpotifyTest {
    public static void main(String[] args) {
        Spotify s = new Spotify();
        s.Login();
        s.getPlaylists();
    }
}
