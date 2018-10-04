import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class Song{

    public int ID;

    String artist;
    String title;
    String album;

    // Song duration in milliseconds
    int duration;

    boolean explicit;
    String spotfyID;
    String spotifyURI;

    enum OriginHostName{
        AMAZON,
        ITUNES,
        SPOTIFY,
        YOUTUBE
    }

    // Only compares song title and artist

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(artist, song.artist) &&
                Objects.equals(title, song.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title);
    }

    @Override
    public String toString() {
        return "Song{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                ", explicit=" + explicit +
                ", spotfyID='" + spotfyID + '\'' +
                ", spotifyURI='" + spotifyURI + '\'' +
                '}';
    }

    // Used to save to db, doesn't save if song already has an ID
    // TODO add functionality for updating songs, possibly only for certain fields
    // Returns true on success, false on failure
    public boolean save(){
        if(ID != 0){
            // Already in db, no need for action
            return true;
        }

        String insertQuery = "INSERT INTO Songs(Artist, Title, Album, Duration, Explicit, SpotifyID, SpotifyURI) "+
                             "VALUES("+ artist + ", "+ title + ", "+ album + ", "+ duration + ", "+ explicit + ", " + spotfyID + ", "+ spotifyURI + ")";

        SqlHelper helper = new SqlHelper();
        helper.ExecuteQuery(insertQuery);

        // Get ID of thing we just inserted
        String idQuery = "SELECT ID FROM Songs WHERE Artist = " + artist + " AND Title = " + title;
        ResultSet resultSet = helper.ExecuteQuery(idQuery);

        try{
            ID = resultSet.getInt("ID");
        }catch (SQLException e){
            // TODO
            return false;
        }

        return true;
    }
}