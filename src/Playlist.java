import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class Playlist{

    public int ID;
    public int UserID;
    public String Name;
    private List<Song> playlist = new ArrayList<>();


    public void addSong(Song song){
        playlist.add(song);
    }

    public Song removeSong(int index){
        return playlist.remove(index);
    }

    public Song getSong(int index){
        return playlist.get(index);
    }

    public int getSize(){ return playlist.size(); }

    public List<Song> getArrayList() {
        return new ArrayList<>(playlist);
    }

    public boolean contains(Song song){ return playlist.contains(song); }

    // Returns a new playlist object that contains songs from both playlists with no duplicates
    // Compares using overridden equals in song object.

    public Playlist merge(Playlist playlist1) {

        Playlist newPlaylist = new Playlist();
        newPlaylist.playlist.addAll(this.playlist);

        for (Song song : playlist1.playlist) {
            if (!newPlaylist.playlist.contains(song))
                newPlaylist.addSong(song);
        }

        return newPlaylist;
    }

    // Fetches songs that are saved in the db for the current playlist object and saves them in the playlist class object
    // Returns true on success, false on failure
    public boolean FetchSongs(){
        if(ID == 0){
            // Not in db
            return false;
        }

        String fetchQuery = "SELECT Songs.* "+
                            "FROM Playlists "+
                                "JOIN PlaylistSongs ON PlaylistSongs.PlaylistID = Playlists.ID "+
                                "JOIN Songs ON Songs.ID = PlaylistSongs.SongID "+
                            "WHERE Playlists.ID = "+ ID;

        SqlHelper helper = new SqlHelper();
        ResultSet resultSet = helper.ExecuteQuery(fetchQuery);

        playlist = new ArrayList<Song>();
        try{
            while(resultSet.next()){
                Song song = new Song();
                song.album = resultSet.getString("Album");
                song.artist = resultSet.getString("Artist");
                song.duration = resultSet.getInt("Duration");
                song.explicit = resultSet.getBoolean("Explicit");
                song.ID = resultSet.getInt("ID");
                song.spotfyID = resultSet.getString("SpotifyID");
                song.spotifyURI = resultSet.getString("SpotifyURI");

                playlist.add(song);
            }
        }catch(SQLException e){
            // TODO
            return false;
        }

        return true;
    }

    // Saves a playlist to the database, returns true on success and false on failure
    public boolean save(){
        if(ID == 0){
            // Playlist hasn't been saved to DB yet

            // TODO Get current user ID, this is a dummy value
            int currentUserID = 1;
            String playlistInsertQuery = "INSERT INTO Playlists(UserID, Name) VALUES(" +  currentUserID + ", " + Name + ")";
            SqlHelper helper = new SqlHelper();
            helper.ExecuteQuery(playlistInsertQuery);

            // Send query to find ID of playlist we just inserted
            String findIDQuery = "SELECT ID FROM Playlists WHERE Name = '" + Name + "'";
            ResultSet results = helper.ExecuteQuery(findIDQuery);

            try{
                ID = results.getInt("ID");
            }catch(SQLException e) {
                // TODO
                return false;
            }
        }

        // We've confirmed that the playlist has an ID at this point, so now we save the songs one by one
        // TODO
        return true;
    }

}