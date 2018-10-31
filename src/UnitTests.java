import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

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
        song.ID = 17;
        song.title = "Title 1";
        song.artist = "Dre";
        song.explicit = true;
        song.duration = 3;
        song.album = "Best of Dre";

        Song song2 = new Song();
        song2.ID = 18;
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

        // Fetch User
        User user = User.getUserByUserName("testUserName");
        user.FetchPlaylists();

        boolean ret;
        for (Playlist list : user.playlistList) {
            ret = list.delete();
            Assert.assertEquals(true, ret);
        }

        ret = playlist.save(user);

        Assert.assertEquals(true, ret);

        user.FetchPlaylists();

        Playlist userPlaylist = user.playlistList.get(0);

        boolean test1 = userPlaylist.Name.equals(playlist.Name);
        boolean test2 = userPlaylist.getSong(0).title.equals(song.title);
        boolean test3 = userPlaylist.getSong(1).title.equals(song2.title);

        Assert.assertEquals(true, test1);
        Assert.assertEquals(true, test2);
        Assert.assertEquals(true, test3);
    }

    @Test(timeout = 100)
    public static void shareTokenTest(){
        Song song = new Song();
        song.ID = 17;
        song.title = "Title 1";
        song.artist = "Dre";
        song.explicit = true;
        song.duration = 3;
        song.album = "Best of Dre";

        Song song2 = new Song();
        song2.ID = 18;
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

        String token = playlist.generateShareToken();

        User user = User.getUserByUserName("testUserName");

        Assert.assertEquals(true, user != null);

        user.FetchPlaylists();

        boolean ret;
        for (Playlist list : user.playlistList) {
            ret = list.delete();
            Assert.assertEquals(true, ret);
        }

        ret = Playlist.generateSharedPlaylist(token, user);

        Assert.assertEquals(true, ret);

        user.FetchPlaylists();

        Assert.assertEquals(user.playlistList.size() == 1, user.playlistList.size() != 1);

        Playlist returnedList = user.playlistList.get(0);

        Assert.assertEquals(true, returnedList.equals(playlist));
        Assert.assertEquals(true, returnedList.getSong(0).equals(song));
        Assert.assertEquals(true, returnedList.getSong(1).equals(song2));
    }
}
