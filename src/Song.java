import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
public class Song {
    public int ID;
    String artist;
    String title;
    String album;
    // Song duration in milliseconds
    int duration;
    boolean explicit;
    String spotfyID;
    String spotifyURI;
    OriginHostName origin;

    public Song(String title) {
        this.title = title;
    }

    public Song() {

    }

    public void setAlbum(String name) {
        this.title = name;
    }

    public void setArtist(String name) {
        this.artist = name;
    }

    public int getID() {
        return ID;
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getAlbum() {
        return album;
    }

    public int getDuration() {
        return duration;
    }

    public boolean isExplicit() {
        return explicit;
    }

    public String getSpotfyID() {
        return spotfyID;
    }

    public String getSpotifyURI() {
        return spotifyURI;
    }

    public OriginHostName getOrigin() {
        return origin;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public void setExplicit(boolean explicit) {
        this.explicit = explicit;
    }

    public void setSpotfyID(String spotfyID) {
        this.spotfyID = spotfyID;
    }

    public void setSpotifyURI(String spotifyURI) {
        this.spotifyURI = spotifyURI;
    }

    public void setOrigin(OriginHostName origin) {
        this.origin = origin;
    }

    enum OriginHostName {
        AMAZON,
        ITUNES,
        SPOTIFY,
        YOUTUBE
    }

    public Song(ResultSet resultSet){
        try {
            album = resultSet.getString("Album");
            artist = resultSet.getString("Artist");
            duration = resultSet.getInt("Duration");
            explicit = resultSet.getBoolean("Explicit");
            ID = resultSet.getInt("ID");
            spotfyID = resultSet.getString("SpotifyID");
            spotifyURI = resultSet.getString("SpotifyURI");
        }catch(SQLException e){
            System.err.println(e);
        }
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
    public boolean save() {
        if (ID != 0) {
            // Already in db, no need for action
            return true;
        }
        String insertQuery = "INSERT INTO Songs(Artist, Title, Album, Duration, Explicit, SpotifyID, SpotifyURI) " +
                "VALUES('" + artist + "', '" + title + "', '" + album + "', " + duration + ", " + explicit + ", '" + spotfyID + "', '" + spotifyURI + "')";
        SqlHelper helper = new SqlHelper();
        helper.ExecuteQuery(insertQuery);
        // Get ID of thing we just inserted
        String idQuery = "SELECT ID FROM Songs WHERE Artist = " + artist + " AND Title = " + title;
        ResultSet resultSet = helper.ExecuteQueryWithReturn(idQuery);
        try {
            while(resultSet.next()){
                ID = resultSet.getInt("ID");
            }
            helper.closeConnection();
        } catch (SQLException e) {
            System.err.println(e);
            return false;
        }
        return true;
    }

    public static Song fetchSongByID(int songID){
        String query = "SELECT * FROM Songs WHERE ID=" + songID;

        SqlHelper helper = new SqlHelper();
        ResultSet resultSet = helper.ExecuteQueryWithReturn(query);

        try{
            while(resultSet.next()){
                return new Song(resultSet);
            }
        }catch (SQLException e){
            System.err.println(e);
            return null;
        }

        return null;
    }
}