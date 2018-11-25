import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
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
    public String youtubeId;
    public String googleId;
    Origin origin;
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

    public String getName(){return Name;}

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
        //String fetchQuery = "SELECT Songs.* " +
        //        "FROM Playlists " +
        //        "JOIN PlaylistSongs ON PlaylistSongs.PlaylistID = Playlists.ID " +
        //        "JOIN Songs ON Songs.ID = PlaylistSongs.SongID " +
        //        "WHERE Playlists.ID = " + ID;
        SqlHelper helper = new SqlHelper();
        //ResultSet resultSet = helper.ExecuteQueryWithReturn(fetchQuery);
        List<Song> songList = new ArrayList<Song>();
        try {
            PreparedStatement fetchStatement = helper.connection.prepareStatement("SELECT Songs.* " +
                    "FROM Playlists " +
                    "JOIN PlaylistSongs ON PlaylistSongs.PlaylistID = Playlists.ID " +
                    "JOIN Songs ON Songs.ID = PlaylistSongs.SongID " +
                    "WHERE Playlists.ID = ?");
            fetchStatement.setInt(1, ID);

            ResultSet resultSet = fetchStatement.executeQuery();

            while (resultSet.next()) {
                Song song = new Song(resultSet);
                songList.add(song);
            }
        } catch (SQLException e) {
            System.out.println(e);
            return null;
        }
        return songList;
    }

    // Saves a playlist to the database, returns true on success and false on failure
    public boolean save(User currentUser) {
        if (ID == 0) {
            // Playlist hasn't been saved to DB yet
            int currentUserID = currentUser.ID;
            SqlHelper helper = new SqlHelper();

            try {
                PreparedStatement playlistInsertStatement = helper.connection.prepareStatement("INSERT INTO Playlists(UserID, Name) VALUES(?,?)");
                playlistInsertStatement.setInt(1, currentUserID);
                playlistInsertStatement.setString(2, Name);

                playlistInsertStatement.execute();

                // Send query to find ID of playlist we just inserted
                PreparedStatement findIDStatement = helper.connection.prepareStatement("SELECT ID FROM Playlists WHERE Name = ?");
                findIDStatement.setString(1, Name);

                ResultSet results = findIDStatement.executeQuery();

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
            SqlHelper helper = new SqlHelper();

            try{
                PreparedStatement updateStatement = helper.connection.prepareStatement("UPDATE Playlists "+
                        "SET Name = ? "+
                        "WHERE ID = ? ");

                updateStatement.setString(1, Name);
                updateStatement.setInt(2, ID);

                updateStatement.execute();
            }catch (SQLException e){
                System.err.println(e);
                return false;
            }
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
            if (!songList.contains(song)) {
                addedList.add(song);
            }
        }
        // Find what songs were deleted, if any
        List<Song> deletedList = new ArrayList<Song>();
        for (Song song : unionList) {
            if (!playlist.contains(song)) {
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
            SqlHelper helper = new SqlHelper();

            try{
                PreparedStatement deletionStatement1 = helper.connection.prepareStatement("DELETE PlaylistHistorySongs FROM PlaylistHistorySongs " +
                        "JOIN PlaylistHistory ON PlaylistHistory.ID = PlaylistHistorySongs.PlaylistHistoryID WHERE PlaylistHistory.PlaylistID = ?");
                deletionStatement1.setInt(1, ID);

                PreparedStatement deletionStatement2 = helper.connection.prepareStatement("DELETE FROM PlaylistHistory WHERE PlaylistHistory.PlaylistID = ?");
                deletionStatement2.setInt(1, ID);

                PreparedStatement deletionStatement3 = helper.connection.prepareStatement("DELETE FROM PlaylistSongs WHERE PlaylistID = ?");
                deletionStatement3.setInt(1, ID);

                PreparedStatement deletionStatement4 = helper.connection.prepareStatement("DELETE FROM Playlists WHERE ID = ?");
                deletionStatement4.setInt(1, ID);

                deletionStatement1.execute();
                deletionStatement2.execute();
                deletionStatement3.execute();
                deletionStatement4.execute();
            }catch (SQLException e){
                System.err.println(e);
                return false;
            }

            helper.closeConnection();

            return true;
        }
    }

    public String generateShareToken(){
        StringBuilder token = new StringBuilder();
        token.append(Name).append(";");
        for(Song song : playlist){
            token.append(song.ID).append(",");
        }
        return token.toString();
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

    private boolean savePlaylistState(){
        List<Song> currentSongs = this.FetchSongs();
        String currentTime = Timestamp.valueOf(LocalDateTime.now()).toString();
        String currentTimeTrunc = currentTime.substring(0, currentTime.indexOf('.') + 4);

        SqlHelper helper = new SqlHelper();

        int historyID = 0;
        try{
            PreparedStatement playlistHistoryStatement = helper.connection.prepareStatement("INSERT INTO PlaylistHistory(PlaylistID, CreatedTime) VALUES(?,?)");
            playlistHistoryStatement.setInt(1, ID);
            playlistHistoryStatement.setString(2, currentTimeTrunc);

            playlistHistoryStatement.execute();

            PreparedStatement idFetchStatement = helper.connection.prepareStatement("SELECT ID FROM PlaylistHistory WHERE PlaylistHistory.PlaylistID = ? ORDER BY CreatedTime DESC");
            idFetchStatement.setInt(1, ID);

            ResultSet resultSet = idFetchStatement.executeQuery();

            while(resultSet.next()){
                historyID = resultSet.getInt("ID");
                break;
            }
        }catch(SQLException e){
            System.err.println(e);
            return false;
        }

        if(historyID == 0) return false;

        for(Song song : currentSongs){
            try{
                PreparedStatement playlistHistorySongInsertStatement = helper.connection.prepareStatement("INSERT INTO PlaylistHistorySongs(PlaylistHistoryID, SongID) VALUES(?,?)");
                playlistHistorySongInsertStatement.setInt(1, historyID);
                playlistHistorySongInsertStatement.setInt(2, song.ID);

                playlistHistorySongInsertStatement.execute();
            }catch (SQLException e){
                System.err.println(e);
                return false;
            }
        }

        helper.closeConnection();

        return true;
    }

    public List<Playlist> fetchPreviousStates(){

        // Start by getting each playlistHistory ID
        SqlHelper helper = new SqlHelper();

        List<Integer> playlistHistoryIDs = new ArrayList<>();

        try{
            PreparedStatement fetchIDStatement = helper.connection.prepareStatement("SELECT * FROM PlaylistHistory WHERE PlaylistID = ? ORDER BY CreatedTime DESC");
            fetchIDStatement.setInt(1, ID);

            ResultSet resultSet = fetchIDStatement.executeQuery();

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
            Playlist previousState = new Playlist();
            previousState.Name = this.Name;
            previousState.ID = this.ID;

            try{
                PreparedStatement songFetchStatement = helper.connection.prepareStatement("SELECT * FROM PlaylistHistorySongs WHERE PlaylistHistoryID = ?");
                songFetchStatement.setInt(1, historyID);

                ResultSet resultSet1 = songFetchStatement.executeQuery();

                while(resultSet1.next()){
                    int songID = resultSet1.getInt("SongID");
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
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement idFetchQuery = helper.connection.prepareStatement("SELECT * FROM Playlists WHERE ID = ?");
            idFetchQuery.setInt(1, id);

            ResultSet resultSet = idFetchQuery.executeQuery();

            if(resultSet.next()){
                Playlist playlist = new Playlist();
                playlist.ID = resultSet.getInt("ID");
                playlist.Name = resultSet.getString("Name");
                return playlist;
            }else{
                return null;
            }
        }catch (SQLException e){
            System.err.println(e);
            return null;
        }
    }

    public void setPlaylist(List<Song> playlist) {
        this.playlist = playlist;
    }

    @Override
    public boolean equals(Object obj) {
        Playlist playlist = (Playlist) obj;
        return this.Name.equals(playlist.Name);
    }
}