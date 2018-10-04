import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.Buffer;
import java.util.*;
import java.net.*;


import com.neovisionaries.i18n.CountryCode;
import com.wrapper.spotify.*;
import com.wrapper.spotify.exceptions.detailed.BadRequestException;
import com.wrapper.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import com.wrapper.spotify.model_objects.credentials.ClientCredentials;
import com.wrapper.spotify.model_objects.specification.*;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import com.wrapper.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import com.wrapper.spotify.requests.data.albums.GetAlbumRequest;
import com.wrapper.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistRequest;
import com.wrapper.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;
import javafx.util.Pair;
import org.apache.http.HttpRequest;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
//import com.google.*;
import javax.swing.*;

public class Spotify extends StreamingService
{
    private static final String client_ID = "150469a5b0d949a9b3693a73edc3b46d";
    private static final String client_Secret = "69e5123c458b43fc94d5d380281aee15";
    private static final String scopes = "user-read-birthdate,user-read-email";

    private static final URI redirectURI = SpotifyHttpManager.makeUri("http://localhost:15000/TESTPARAMETER");//temporary redirect uri until this functionality is implemented

    private static final SpotifyApi spotifyApi = new SpotifyApi.Builder()
            .setClientId(client_ID)
            .setClientSecret(client_Secret)
            .setRedirectUri(redirectURI)
            .build();

    //Used for testing, possibly useful in the future for search functionality
    //private static final ClientCredentialsRequest cCR = spotifyApi.clientCredentials().build();

    private static final AuthorizationCodeUriRequest aCUR = spotifyApi.authorizationCodeUri().scope(scopes).show_dialog(true).build();

    //URL the user is sent to so they can allow us to access their account
    public String getAuthorizationURL(){
        final URI temp = aCUR.execute();
        return temp.toString();
    }

    //Code retrieved from user authorization is passed, return value is a pair <accessToken, refreshToken>
    public Pair<String,String> Login(String code) {
        try {


            /*ServerSocket ss = new ServerSocket(15000);
            Socket client = ss.accept();
            BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
            String input = in.readLine();
            System.out.println(input);
            String code = input.split(" ")[1].substring(7);
*/

            /*/// Only for purposes of making sure program functions until
            // a redirect URI can be set up, to test, copy the code from
            // the current URI redirect into the GUI
            JFrame frame = new JFrame();
            JTextField text = new JTextField();
            text.setEnabled(true);
            text.setEditable(true);
            frame.add(text);
            frame.show();
            Thread.sleep(20000); //waits for 20 seconds to give tester time to login and copy code into GUI
            ///*/

            final AuthorizationCodeRequest authorizationcoderequest = spotifyApi.authorizationCode(code).grant_type("authorization_code").build();
            AuthorizationCodeCredentials c = authorizationcoderequest.execute();

            Pair<String, String> tokens = new Pair<String, String> (c.getAccessToken(),c.getRefreshToken());

            return tokens;
        }
        catch (BadRequestException e){
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return new Pair<String, String> ("error","error");
    }
    public Playlist[] getPlaylists(Pair<String, String> tokens)
    {
        try {
            final GetListOfCurrentUsersPlaylistsRequest getPlaylistsRequest = spotifyApi
                    .getListOfCurrentUsersPlaylists()
                    .limit(10)
                    .offset(0)
                    .build();
            final Paging<PlaylistSimplified> playlists = getPlaylistsRequest.execute();
            PlaylistSimplified playlist_list[] = playlists.getItems();
            Playlist[] return_lists = new Playlist [playlists.getTotal()];

        } catch (Exception e) {e.printStackTrace();}
        return new Playlist[] {};
    }
    
}
