import com.google.api.client.http.HttpStatusCodes;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Youtube extends StreamingService {

    private static final String client_id = "1079317001744-dgc6eni8lhuvrtf6g0blghcm8n9u4one.apps.googleusercontent.com";
    private static final String client_secret = "bpFp-zkeuu8KM8HMj6Qe_l77";
    private static final String baseUrl = "https://www.googleapis.com/youtube/v3/";
    private static final String apiKey = "AIzaSyD_Ntid7tXAXzPyONEDsPxvGBNQlWu98gQ";

    // Create a token from code given from oauth2 login
    static String GetToken(String code) throws Exception {

        // Create Http Objects
        HttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("https://www.googleapis.com/oauth2/v4/token");

        // Add Header
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

        // Create Parameters
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("client_id", client_id));
        params.add(new BasicNameValuePair("client_secret", client_secret));
        params.add(new BasicNameValuePair("redirect_uri", "http://localhost/login/?platformID=Youtube"));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));

        // Add Parameters
        httpPost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        // Execute
        HttpResponse response = httpclient.execute(httpPost);

        // If failed, throw exception
        if(response.getStatusLine().getStatusCode() != HttpStatusCodes.STATUS_CODE_OK)
            throw new Exception("Got status code: " + response.getStatusLine().getStatusCode());

        // Get response in string
        HttpEntity entity = response.getEntity();
        String string = EntityUtils.toString(entity);

        // Convert string to json
        JSONObject jsonObj = new JSONObject(string);

        // Get token from json
        return (String) jsonObj.get("access_token");
    }

    static List<Playlist> getPlaylists(String token) throws Exception {

        // Build query
        QueryValues queryValues = new QueryValues();
        queryValues.put("maxResults", "25");
        queryValues.put("access_token", token);
        queryValues.put("part", "snippet");
        queryValues.put("mine", "true");
        queryValues.put("key", apiKey);

        // Send http request
        String urlString = baseUrl + "playlists" + queryValues.toQueryString();
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        InputStream in = connection.getInputStream();

        // Get result and put in string
        Scanner s = new Scanner(in).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        // Convert result to json objects
        JSONObject json = new JSONObject(result);
        JSONArray jsonArray = json.getJSONArray("items");

        // Iterate through json playlist objects and convert them to java playlist objects
        List<Playlist> playlists = new ArrayList<>();
        for(int i = 0; i < jsonArray.length(); i++){
            Playlist playlist = new Playlist();
            JSONObject jsonPlaylist = jsonArray.getJSONObject(i);
            String name = jsonPlaylist.getJSONObject("snippet").getString("title");
            String id = jsonPlaylist.getString("id");
            playlist.setName(name);
            playlist.youtubeId = id;
            playlist.origin = Origin.YOUTUBE;
            playlists.add(playlist);
        }

        return playlists;
    }

    static List<Song> importPlaylist(String token, String playlistName) throws Exception {
        // Find the right playlist
        List<Playlist> playlists = getPlaylists(token);
        Playlist playlist = null;
        for(Playlist p : playlists){
            if(p.Name.equals(playlistName)){
                playlist = p;
                break;
            }
        }
        if (playlist == null) throw new Exception("Couldn't find youtube playlist: " + playlistName);

        // Build query
        QueryValues queryValues = new QueryValues();
        queryValues.put("access_token", token);
        queryValues.put("part", "snippet");
        queryValues.put("key", apiKey);
        queryValues.put("maxResults", "25");
        queryValues.put("playlistId", playlist.youtubeId);

        JSONObject json;
        List<JSONArray> songJsonArrays = new ArrayList<>();

        do {
            // Send http request
            String urlString = baseUrl + "playlistItems" + queryValues.toQueryString();
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            InputStream in = connection.getInputStream();

            // Get result and put in string
            Scanner s = new Scanner(in).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";

            // Convert result into json objects
            json = new JSONObject(result);
            songJsonArrays.add(json.getJSONArray("items"));

            // Put the next page token in for the next query
            if(json.has("nextPageToken"))
                queryValues.put("pageToken", json.getString("nextPageToken"));

        } while(json.has("nextPageToken"));

        // Iterate through json playlist item objects and convert them into java song objects
        List<Song> songs = new ArrayList<>();
        for(JSONArray jsonArray : songJsonArrays) {
            for (int i = 0; i < jsonArray.length(); i++) {
                Song song = new Song();
                JSONObject songJson = jsonArray.getJSONObject(i);
                String title = songJson.getJSONObject("snippet").getString("title");

                // Remove parts in brackets (was usually stuff like (official music video))
                title = title.replaceAll("\\[.*]","");
                title = title.replaceAll("\\(.*\\)","");
                song.title = title;
                song.youtubeId = songJson.getString("id");
                song.origin = Origin.YOUTUBE;

                songs.add(song);
            }
        }

        return songs;
    }
}