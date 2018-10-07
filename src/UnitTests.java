import org.junit.Assert;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UnitTests {

    public static void main(String[] args){
        UserCreationTest1();
        PlaylistTest();
    }

    @Test(timeout = 100)
    public static void UserCreationTest1(){
        String userName = "testUserName";
        String password = "testPassword";
        String email = "testEmail@test.com";

        User user = User.CreateUser(userName, password, email);

        boolean actual = UserPassword.IsPasswordCorrect(userName, password);

        SqlHelper helper = new SqlHelper();
        String deletionQuery = "DELETE FROM UserPasswords WHERE UserID = '" + user.ID + "'";
        String deletionQuery2 = "DELETE FROM Users WHERE ID = '" + user.ID + "'";

        helper.ExecuteQuery(deletionQuery);
        helper.ExecuteQuery(deletionQuery2);

        helper.closeConnection();

        Assert.assertEquals(true, actual);
    }

    @Test(timeout = 100)
    public static void PlaylistTest(){
        Song song = new Song();
        song.title = "Title 1";
        song.artist = "Dre";
        song.explicit = true;
        song.duration = 3;
        song.album = "Best of Dre";

        Song song2 = new Song();
        song2.title = "Title 2";
        song2.album = "What what";
        song2.duration = 4;
        song2.explicit = false;
        song2.artist = "me";

        Playlist playlist = new Playlist();
        playlist.UserID = 41;
        playlist.Name = "Test Playlist";
        playlist.addSong(song);
        playlist.addSong(song2);
        playlist.save();

        // Fetch User
        User user = User.getUserByUserName("testUserName");
        user.FetchPlaylists();

        Playlist userPlaylist = user.playlistList.get(0);

        boolean test1 = userPlaylist.Name.equals(playlist.Name);
        boolean test2 = userPlaylist.getSong(0).equals(song);
        boolean test3 = userPlaylist.getSong(1).equals(song2);

        Assert.assertEquals(true, test1);
        Assert.assertEquals(true, test2);
        Assert.assertEquals(true, test3);
    }
}
