import org.apache.commons.lang3.tuple.MutablePair;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class User {

    int ID;
    String userName;
    String email;
    MutablePair<String, String> spotifyTokens;
    String youtubeToken;
    String spotifyToken;
    String googleMusicToken;
    ArrayList<Playlist> playlistList = new ArrayList<>();

    public User(int ID, String userName, String email){
        this.ID = ID;
        this.userName = userName;
        this.email = email;
        spotifyTokens = null;
    }

    public User(ResultSet resultSet){
        try{
            ID = resultSet.getInt("ID");
            userName = resultSet.getString("UserName");
            email = resultSet.getString("Email");
            spotifyToken = resultSet.getString("SpotifyToken");
            youtubeToken = resultSet.getString("YoutubeToken");
            googleMusicToken = resultSet.getString("GoogleMusicToken");
        }catch (SQLException e){
            System.err.println(e);
        }
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
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement userStatement = helper.connection.prepareStatement("SELECT * FROM Users WHERE UserName = ?");
            userStatement.setString(1, userName);

            ResultSet resultSet = userStatement.executeQuery();

            if(resultSet.next()){
                return new User(resultSet);
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

        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement insertionStatement = helper.connection.prepareStatement("INSERT INTO Users([UserName], [Email], [CreatedDate]) VALUES(?,?,?)");
            insertionStatement.setString(1, newUser.userName);
            insertionStatement.setString(2, newUser.email);
            insertionStatement.setString(3, sqlDate.toString());

            insertionStatement.execute();

            PreparedStatement fetchIDStatement = helper.connection.prepareStatement("SELECT ID FROM Users WHERE UserName = ?");
            fetchIDStatement.setString(1, newUser.userName);

            ResultSet results = fetchIDStatement.executeQuery();

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
        SqlHelper helper = new SqlHelper();

        playlistList = new ArrayList<Playlist>();
        try{
            PreparedStatement fetchStatement = helper.connection.prepareStatement("SELECT Playlists.* FROM Playlists WHERE UserID = ?");
            fetchStatement.setInt(1, ID);

            ResultSet resultSet = fetchStatement.executeQuery();

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

    public boolean setAuthToken(String token){
        SqlHelper helper = new SqlHelper();

        // Generate Salt
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[50];
        random.nextBytes(salt);

        // Append salt to Token
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < salt.length; ++i) {
            sb.append(Integer.toHexString((salt[i] & 0xFF) | 0x100).substring(1, 3));
        }
        String saltString = sb.toString();
        String saltPlusPass = saltString + token;

        // Hash Salt + token
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] saltedHash = digest.digest(saltPlusPass.getBytes(StandardCharsets.UTF_8));
            sb = new StringBuilder();
            for (int i = 0; i < saltedHash.length; ++i) {
                sb.append(Integer.toHexString((saltedHash[i] & 0xFF) | 0x100).substring(1, 3));
            }

            try {
                PreparedStatement statement = helper.connection.prepareStatement("UPDATE Users SET AuthToken = ? WHERE ID = ?");
                statement.setString(1, sb.toString());
                statement.setInt(2, ID);

                PreparedStatement statement2 = helper.connection.prepareStatement("UPDATE Users SET AuthTokenSalt = ? WHERE ID = ?");
                statement2.setString(1, saltString);
                statement2.setInt(2, ID);

                statement.execute();
                statement2.execute();
            }catch (SQLException e){
                System.err.println(e);
                return false;
            }

        }catch (NoSuchAlgorithmException e){
            System.err.println(e);
            return false;
        }

        return true;
    }

    public boolean isAuthTokenValid(String token){
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement fetchStatement = helper.connection.prepareStatement("SELECT * FROM USERS WHERE ID = ?");
            fetchStatement.setInt(1, ID);

            ResultSet resultSet = fetchStatement.executeQuery();

            String AuthToken = "";
            String Salt = "";
            while(resultSet.next()){
                AuthToken = resultSet.getString("AuthToken");
                Salt = resultSet.getString("AuthTokenSalt");
            }

            // Append salt to Password
            String saltPlusPass = Salt + token;

            // Hash Salt + Password
            String userInputtedPasswordHashed = "";
            try{
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] saltedHash = digest.digest(saltPlusPass.getBytes(StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < saltedHash.length; ++i) {
                    sb.append(Integer.toHexString((saltedHash[i] & 0xFF) | 0x100).substring(1, 3));
                }

                if(sb.toString().equals(AuthToken)){
                    return true;
                }else{
                    return false;
                }

            }catch(NoSuchAlgorithmException e) {
                System.err.println(e);
                return false;
            }

        }catch (SQLException e){
            System.err.println(e);
            return false;
        }
    }

    public boolean updateSpotifyToken(String token){
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement update = helper.connection.prepareStatement("UPDATE USERS SET SpotifyToken = ? WHERE ID = ?");
            update.setString(1, token);
            update.setInt(2, ID);

            update.execute();
        }catch (SQLException e){
            System.err.println(e);
            return false;
        }

        return true;
    }

    public boolean updateGoogleMusicToken(String token){
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement update = helper.connection.prepareStatement("UPDATE USERS SET GoogleMusicToken = ? WHERE ID = ?");
            update.setString(1, token);
            update.setInt(2, ID);

            update.execute();
        }catch (SQLException e){
            System.err.println(e);
            return false;
        }

        return true;
    }

    public boolean updateYoutubeToken(String token){
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement update = helper.connection.prepareStatement("UPDATE USERS SET YoutubeToken = ? WHERE ID = ?");
            update.setString(1, token);
            update.setInt(2, ID);

            update.execute();
        }catch (SQLException e){
            System.err.println(e);
            return false;
        }

        return true;
    }

    public boolean changePassword(String newPassword){
        // First, delete UserPassword entry
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement deletion = helper.connection.prepareStatement("DELETE FROM UserPasswords WHERE UserID = ?");
            deletion.setInt(1, ID);

            deletion.execute();
        }catch (SQLException e){
            System.err.println(e);
            return  false;
        }

        // Create New UserPassword
        return UserPassword.CreateUserPassword(ID, newPassword);
    }
}
