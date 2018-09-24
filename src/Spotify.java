import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.*;
import java.net.*;

import org.json.JSONObject;//package to handle creation of JSON objects for API communication

public class Spotify extends StreamingService
{
    private final String clientID = "150469a5b0d949a9b3693a73edc3b46d";
    private final String clientSecret = "69e5123c458b43fc94d5d380281aee15";
    private final List<String> scopes = Arrays.asList("");//scopes to which application requires access
    public void Login() {
        //current code returns 400 error,
        try {
            JSONObject p = new JSONObject();
            JSONObject response = new JSONObject();
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            HttpURLConnection.setFollowRedirects(true);
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            p.put("grant_type", "client_credentials");
            OutputStream out = connection.getOutputStream();
            out.write("POST".getBytes());
            String temp = clientID+":"+clientSecret;
            Base64.Encoder encode = Base64.getEncoder();
            out.write(("Authorization: Basic" + encode.encodeToString(temp.getBytes())).getBytes());
            out.write(p.toString().getBytes());

            /*
            401 error, request is formatted correctly but lacks an access token
            out.write(("artists/1vCWHaC5f2uS3yhpwWbIA6/albums?album_type=SINGLE&offset=20&limit=10&client_id="+clientID+"&client_secret="+clientSecret).getBytes());
            out.write(p.toString().getBytes());
            out.write(("GET https://accounts.spotify.com/authorize/?client_id="+clientID+"&response_type=code&redirect_uri="+"https://www.google.com"+"&scope=user-read-private%20user-read-email").getBytes());
            */

            System.out.println("p=" + p.toString());
            System.out.println(connection.getResponseMessage());
            System.out.println(connection.getResponseCode());
            //BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
