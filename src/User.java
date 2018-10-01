import com.sun.media.jfxmedia.events.PlayerStateListener;

import java.util.List;
import java.sql.*;

public class User {

    int ID;
    String userName;
    String email;
    List<Playlist> playlistList;

    public User(String userName, String password, String email){
        this.userName = userName;
        this.email = email;

        java.util.Date currentDate = new java.util.Date();
        Date sqlDate = new Date(currentDate.getTime());

        String insertion = "INSERT INTO Users([UserName], [Email], [CreatedDate])" +
                           "VALUES(" + this.userName + ", " + this.email + ", " + sqlDate + ")";

        SqlHelper helper = new SqlHelper();

        helper.ExecuteQuery(insertion);

        // Fetch ID back, make UserPassword instance
        String idFetchQuery = "SELECT ID FROM Users WHERE UserName = " + this.userName;
        ResultSet results = helper.ExecuteQuery(idFetchQuery);

        try{
            this.ID = results.getInt("ID");
        }catch(SQLException e){
            // TODO
        }

        UserPassword userPassword = new UserPassword(this.ID, password);
    }
}
