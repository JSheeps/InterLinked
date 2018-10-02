import com.sun.media.jfxmedia.events.PlayerStateListener;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class User {

    int ID;
    String userName;
    String email;
    List<Playlist> playlistList = new ArrayList<Playlist>();

    public User(int ID, String userName, String email){
        this.ID = ID;
        this.userName = userName;
        this.email = email;
    }

    // Creates a new User, returns true on success and false on failure
    public static boolean CreateUser(String userName, String password, String email){
        User newUser = new User(0, userName, email);

        java.util.Date currentDate = new java.util.Date();
        Date sqlDate = new Date(currentDate.getTime());

        String insertion = "INSERT INTO Users([UserName], [Email], [CreatedDate])" +
                           "VALUES(" + newUser.userName + ", " + newUser.email + ", " + sqlDate + ")";

        SqlHelper helper = new SqlHelper();

        helper.ExecuteQuery(insertion);

        // Fetch ID back, make UserPassword instance
        String idFetchQuery = "SELECT ID FROM Users WHERE UserName = " + newUser.userName;
        ResultSet results = helper.ExecuteQuery(idFetchQuery);

        try{
            newUser.ID = results.getInt("ID");
        }catch(SQLException e){
            // TODO
        }

        return UserPassword.CreateUserPassword(newUser.ID, password);
    }

    // Fetches playlists that are saved in the db for the current user object and saves them in the playlistList class object
    // Also fetches each song in each playlist it finds
    // Returns true on success, false on failure
    public boolean FetchPlaylists(){
        if(ID == 0){
            // Not in db
            return false;
        }

        String fetchQuery = "SELECT Playlists.* FROM Playlists WHERE UserID = "+ ID;

        SqlHelper helper = new SqlHelper();
        ResultSet resultSet = helper.ExecuteQuery(fetchQuery);

        playlistList = new ArrayList<Playlist>();
        try{
            while(resultSet.next()){
                Playlist playlist = new Playlist();

                playlist.ID = resultSet.getInt("ID");
                playlist.Name = resultSet.getString("Name");
                playlist.UserID = resultSet.getInt("UserID");

                playlist.FetchSongs();

                playlistList.add(playlist);
            }
        }catch(SQLException e){
            // TODO
            return false;
        }

        return true;
    }
}
