import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
class Playlist {
    public int ID;
    public int UserID;
    public String Name;
    private List<Song> playlist = new ArrayList<>();

    public void addSong(Song song) {
        playlist.add(song);
    }

    public Song removeSong(int index) {
        return playlist.remove(index);
    }

    public Song getSong(int index) {
        return playlist.get(index);
    }

    public int getSize() {
        return playlist.size();
    }

    public List<Song> getArrayList() {
        return new ArrayList<>(playlist);
    }

    public boolean contains(Song song) {
        return playlist.contains(song);
    }

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

    // Fetches songs that are saved in the db for the current playlist object
    // Returns song list on success, null on failure
    public List<Song> FetchSongs() {
        if (ID == 0) {
            // Not in db
            return null;
        }
        String fetchQuery = "SELECT Songs.* " +
                "FROM Playlists " +
                "JOIN PlaylistSongs ON PlaylistSongs.PlaylistID = Playlists.ID " +
                "JOIN Songs ON Songs.ID = PlaylistSongs.SongID " +
                "WHERE Playlists.ID = " + ID;
        SqlHelper helper = new SqlHelper();
        ResultSet resultSet = helper.ExecuteQueryWithReturn(fetchQuery);
        List<Song> songList = new ArrayList<Song>();
        try {
            while (resultSet.next()) {
                Song song = new Song();
                song.album = resultSet.getString("Album");
                song.artist = resultSet.getString("Artist");
                song.duration = resultSet.getInt("Duration");
                song.explicit = resultSet.getBoolean("Explicit");
                song.ID = resultSet.getInt("ID");
                song.spotfyID = resultSet.getString("SpotifyID");
                song.spotifyURI = resultSet.getString("SpotifyURI");
                songList.add(song);
            }
        } catch (SQLException e) {
            // TODO
            return null;
        }
        return songList;
    }

    // Saves a playlist to the database, returns true on success and false on failure
    public boolean save() {
        if (ID == 0) {
            // Playlist hasn't been saved to DB yet
            // TODO Get current user ID, this is a dummy value
            int currentUserID = 1;
            String playlistInsertQuery = "INSERT INTO Playlists(UserID, Name) VALUES(" + currentUserID + ", " + Name + ")";
            SqlHelper helper = new SqlHelper();
            helper.ExecuteQuery(playlistInsertQuery);
            // Send query to find ID of playlist we just inserted
            String findIDQuery = "SELECT ID FROM Playlists WHERE Name = '" + Name + "'";
            ResultSet results = helper.ExecuteQueryWithReturn(findIDQuery);
            try {
                ID = results.getInt("ID");
            } catch (SQLException e) {
                // TODO
                return false;
            }
            for (Song song : playlist) {
                PlaylistSong playlistSong = new PlaylistSong(ID, song.ID);
                playlistSong.save();
            }
        }
        // We've confirmed that the playlist has an ID at this point, so now we save the songs one by one
        // Get current state of playlist in db
        List<Song> songList = this.FetchSongs();
        // Find what songs were added, if any
        Set<Song> songSet = new HashSet<Song>();
        songSet.addAll(this.playlist);
        songSet.addAll(songList);
        List<Song> unionList = new ArrayList<Song>(songSet);
        List<Song> addedList = new ArrayList<Song>();
        for (Song song : unionList) {
            if (!playlist.contains(song)) {
                addedList.add(song);
            }
        }
        // Find what songs were deleted, if any
        List<Song> deletedList = new ArrayList<Song>();
        for (Song song : unionList) {
            if (!songList.contains(song)) {
                deletedList.add(song);
            }
        }
        // Add each song in addedList
        for (Song song : addedList) {
            PlaylistSong playlistSong = new PlaylistSong(ID, song.ID);
            boolean ret = playlistSong.save();
            if (ret == false) return false;
        }
        // Delete each song in deletedList
        for (Song song : deletedList) {
            PlaylistSong playlistSong = new PlaylistSong(ID, song.ID);
            boolean ret = playlistSong.delete();
            if (ret == false) return false;
        }
        return true;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public int getNumSongs(){return playlist.size();}

}