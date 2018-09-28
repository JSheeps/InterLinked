import com.sun.media.jfxmedia.events.PlayerStateListener;

import java.util.List;

public class User {

    public User(String userName, String password, String email){
        this.userName = userName;
        this.email = email;

        // TODO save to db

        // Fetch ID back, make UserPassword instance

        // TODO Dummy ID for now
        this.ID = 1;

        UserPassword userPassword = new UserPassword(this.ID, password);
    }

    int ID;
    String userName;
    String email;
    List<Playlist> playlistList;


}
