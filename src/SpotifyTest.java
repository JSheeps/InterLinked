import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Douglas on 10/4/2018.
 */
public class SpotifyTest {

    public static void main(String[] args) {
        String code = "";
        Spotify s = new Spotify();
        try {
            System.out.println(s.getAuthorizationURL());
            ServerSocket ss = new ServerSocket(15000);
            Socket client = ss.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            code = in.readLine();

            code = code.split(" ")[1];
            code = code.substring(7);
            in.close();
        } catch (Exception e) {e.printStackTrace();}
        //<accessToken,refreshToken>
        final Pair<String,String> tokens = s.Login(code);
        Playlist [] lists = s.importPlaylists(tokens);
        Playlist mergedlist = lists[0].merge(lists[1]);
        mergedlist.setName("MergedList");
        s.exportPlaylist(tokens,mergedlist);
    }
}
