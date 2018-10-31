import org.apache.commons.lang3.tuple.MutablePair;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class User {

    int ID;
    String userName;
    String email;
    MutablePair<String, String> tokens;
    ArrayList<Playlist> playlistList = new ArrayList<>();

    public User(int ID, String userName, String email){
        this.ID = ID;
        this.userName = userName;
        this.email = email;
        tokens = null;
    }

    public Playlist getPlaylistById(int id){
        for(Playlist p : playlistList){
            if(p.ID == id)
                return p;
        }

        return null;
    }

    // Gets user data from database and creates new user object
    public static User getUserByUserName(String userName){
        String userQuery = "SELECT * FROM Users WHERE UserName = '" + userName + "'";
        SqlHelper helper = new SqlHelper();

        ResultSet resultSet = helper.ExecuteQueryWithReturn(userQuery);

        try{
            if(resultSet.next()){
                int id = resultSet.getInt("ID");
                String usrrName = resultSet.getString("UserName");
                String email = resultSet.getString("Email");

                return new User(id, usrrName, email);
            }else{
                return null;
            }
        }catch (SQLException e){
            System.err.println(e);
            return null;
        }
    }

    // Creates a new User, returns new user object, or null on failure
    public static User CreateUser(String userName, String password, String email){
        User newUser = new User(0, userName, email);

        java.util.Date currentDate = new java.util.Date();
        Date sqlDate = new Date(currentDate.getTime());

        String insertion = "INSERT INTO Users([UserName], [Email], [CreatedDate]) " +
                           "VALUES(" + "'" + newUser.userName +"'" +  ", " +"'" +  newUser.email +"'" +  ", " + "'" + sqlDate + "'" + ")";

        SqlHelper helper = new SqlHelper();

        helper.ExecuteQuery(insertion);

        // Fetch ID back, make UserPassword instance
        String idFetchQuery = "SELECT ID FROM Users WHERE UserName = '" + newUser.userName + "'";
        ResultSet results = helper.ExecuteQueryWithReturn(idFetchQuery);

        try{
            while(results.next()){
                newUser.ID = results.getInt("ID");
            }
        }catch(SQLException e){
            System.err.println(e);
            return null;
        }

        if(UserPassword.CreateUserPassword(newUser.ID, password)){
            return newUser;
        }
        else
            return null;
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
        ResultSet resultSet = helper.ExecuteQueryWithReturn(fetchQuery);

        playlistList = new ArrayList<Playlist>();
        try{
            while(resultSet.next()){
                Playlist playlist = new Playlist();

                playlist.ID = resultSet.getInt("ID");
                playlist.Name = resultSet.getString("Name");
                playlist.UserID = resultSet.getInt("UserID");

                List<Song> songs = playlist.FetchSongs();

                for(Song song : songs){
                    playlist.addSong(song);
                }

                playlistList.add(playlist);
            }
            helper.closeConnection();
        }catch(SQLException e){
            System.err.println(e);
            return false;
        }

        return true;
    }

    Playlist getPlaylistByName(String name){
        for (Playlist playlist : playlistList){
            if(playlist.Name.equals(name))
                return playlist;
        }
        return null;
    }
}
