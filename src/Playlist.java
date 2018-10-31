import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
class Playlist {
    public int ID;
    public int UserID;
    public String Name;
    public String spotifyId;
    private List<Song> playlist = new ArrayList<>();

    public void clearSongs(){
        playlist.clear();
    }

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
                Song song = new Song(resultSet);

                songList.add(song);
            }
        } catch (SQLException e) {
            System.err.println(e);
            return null;
        }
        return songList;
    }

    // Saves a playlist to the database, returns true on success and false on failure
    public boolean save(User currentUser) {
        if (ID == 0) {
            // Playlist hasn't been saved to DB yet
            int currentUserID = currentUser.ID;
            String playlistInsertQuery = "INSERT INTO Playlists(UserID, Name) VALUES(" + currentUserID + ", '" + Name + "')";
            SqlHelper helper = new SqlHelper();
            helper.ExecuteQuery(playlistInsertQuery);
            // Send query to find ID of playlist we just inserted
            String findIDQuery = "SELECT ID FROM Playlists WHERE Name = '" + Name + "'";
            ResultSet results = helper.ExecuteQueryWithReturn(findIDQuery);
            try {
                while(results.next()){
                    ID = results.getInt("ID");
                }
                helper.closeConnection();
            } catch (SQLException e) {
                System.err.println(e);
                return false;
            }
            for (Song song : playlist) {
                song.save();
                PlaylistSong playlistSong = new PlaylistSong(ID, song.ID);
                playlistSong.save();
            }

            return true;
        }else{
            // Send update query that may or may not actually do anything
            String updateQuery = "UPDATE Playlists "+
                                 "SET Name = '"+ Name + "'"+
                                 "WHERE ID = " + ID;
            SqlHelper helper = new SqlHelper();
            helper.ExecuteQuery(updateQuery);
            helper.closeConnection();
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

        if(addedList.size() != 0 || deletedList.size() != 0){
            savePlaylistState();
        }

        // Add each song in addedList
        for (Song song : addedList) {
            song.save();
            PlaylistSong playlistSong = new PlaylistSong(ID, song.ID);
            boolean ret = playlistSong.save();
            if (ret == false) return false;
        }
        // Delete each song in deletedList
        for (Song song : deletedList) {
            song.save();
            PlaylistSong playlistSong = new PlaylistSong(ID, song.ID);
            boolean ret = playlistSong.delete();
            if (ret == false) return false;
        }
        return true;
    }

    public boolean delete(){
        if(ID == 0){
            return true;
        }else{
            String deletionQuery1 = "DELETE PlaylistHistorySongs FROM PlaylistHistorySongs JOIN PlaylistHistory ON PlaylistHistorySongs.PlaylistHistoryID = PlaylistHistory.ID "+
                    "WHERE PlaylistHistory.PlaylistID =" + ID;
            String deletionQuery2 = "DELETE FROM PlaylistHistory WHERE PlaylistID = " + ID;
            String deletionQuery3 = "DELETE FROM PlaylistSongs WHERE PlaylistID = "+ ID;
            String deletionQuery4 = "DELETE FROM Playlists WHERE ID ="+ ID;

            SqlHelper helper = new SqlHelper();
            helper.ExecuteQuery(deletionQuery1);
            helper.ExecuteQuery(deletionQuery2);
            helper.ExecuteQuery(deletionQuery3);
            helper.ExecuteQuery(deletionQuery4);
            helper.closeConnection();

            return true;
        }
    }

    public String generateShareToken(){
        String token = "";
        token += Name + ";";
        for(Song song : playlist){
            token += song.ID + ",";
        }
        return token;
    }

    // Adds the generated playlist to the User's saved playlists. Fetch playlists again to get the new playlist
    // Returns true on success and false on failure
    public static boolean generateSharedPlaylist(String shareToken, User currentUser){
        String[] bigSplit = shareToken.split(";");

        if(bigSplit.length != 2) return false;

        Playlist newPlaylist = new Playlist();
        newPlaylist.UserID = currentUser.ID;
        newPlaylist.Name = bigSplit[0];
        newPlaylist.playlist = new ArrayList<Song>();

        String[] songIDStrings = bigSplit[1].split(",");
        int[] songIDs = new int[songIDStrings.length];

        for(int i=0;i<songIDStrings.length;i++){
            songIDs[i] = Integer.parseInt(songIDStrings[i]);
        }

        for(int songID : songIDs){
            Song song = Song.fetchSongByID(songID);
            if(song == null) return false;
            newPlaylist.playlist.add(song);
        }

        return newPlaylist.save(currentUser);
    }

    public boolean savePlaylistState(){
        List<Song> currentSongs = this.FetchSongs();

        // Check to make sure all songs in playlist have IDs
        for(Song song : playlist){
            if(song.ID == 0){
                System.err.println("Each song in playlist must be saved prior to calling savePlaylistState()");
                return false;
            }
        }

        String currentTime = LocalDateTime.now().toString();

        String playlistHistoryInsert = "INSERT INTO PlaylistHistory(PlaylistID, CreatedTime) VALUES(" + this.ID + ", '" + currentTime + "')";

        SqlHelper helper = new SqlHelper();
        helper.ExecuteQuery(playlistHistoryInsert);

        // Fetch ID of playlistHistory object
        String idFetch = "SELECT ID FROM PlaylistHistory WHERE PlaylistHistory.PlaylistID = " + this.ID + " ORDER BY CreatedTime DESC";

        ResultSet resultSet = helper.ExecuteQueryWithReturn(idFetch);
        int historyID = 0;
        try{
            while(resultSet.next()){
                historyID = resultSet.getInt("ID");
                break;
            }
        }catch(SQLException e){
            System.err.println(e);
            return false;
        }

        if(historyID == 0) return false;

        for(Song song : playlist){
            String playlistHistorySongInsert = "INSERT INTO PlaylistHistorySongs(PlaylistHistoryID, SongID) VALUES(" + historyID + ", " + song.ID + ")";

            helper.ExecuteQuery(playlistHistorySongInsert);
        }

        helper.closeConnection();

        return true;
    }

    public List<Playlist> fetchPreviousStates(){

        // Start by getting each playlistHistory ID
        String playlistHistoryIDFetch = "SELECT * FROM PlaylistHistory WHERE PlaylistID =" + ID + " ORDER BY CreatedDate DESC";

        SqlHelper helper = new SqlHelper();
        ResultSet resultSet = helper.ExecuteQueryWithReturn(playlistHistoryIDFetch);

        List<Integer> playlistHistoryIDs = new ArrayList<>();

        try{
            while(resultSet.next()){
                playlistHistoryIDs.add(resultSet.getInt("ID"));
            }
        }catch (SQLException e){
            System.err.println(e);
            return  null;
        }

        // Next, fetch the songs in each playlistHistory state and make a new playlist using them
        List<Playlist> previousPlaylistStates = new ArrayList<>();
        for(int historyID : playlistHistoryIDs){
            String songFetch = "SELECT * FROM PlalistHistorySongs WHERE PlaylistHistoryID = " + historyID;

            ResultSet resultSet1 = helper.ExecuteQueryWithReturn(songFetch);

            Playlist previousState = new Playlist();
            previousState.Name = this.Name;
            previousState.ID = this.ID;

            try{
                while(resultSet.next()){
                    int songID = resultSet.getInt("SongID");
                    previousState.playlist.add(Song.fetchSongByID(songID));
                }
            }catch(SQLException e){
                System.err.println(e);
                return null;
            }

            previousPlaylistStates.add(previousState);
        }

        return previousPlaylistStates;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public int getNumSongs(){return playlist.size();}

    public static Playlist getPlaylistById(int id){
        String userQuery = "SELECT * FROM Playlists WHERE ID = '" + id + "'";
        SqlHelper helper = new SqlHelper();

        ResultSet resultSet = helper.ExecuteQueryWithReturn(userQuery);

        try{
            if(resultSet.next()){
                Playlist playlist = new Playlist();
                playlist.ID = resultSet.getInt("ID");
                playlist.Name = resultSet.getString("Name");
                return playlist;
            }else{
                return null;
            }
        }catch (SQLException e){
            // TODO
            System.err.println(e);
            return null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        Playlist playlist = (Playlist) obj;
        return this.Name.equals(playlist.Name);
    }
}