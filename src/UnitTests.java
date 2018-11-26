import org.junit.Assert;
import org.junit.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class UnitTests {
    public static void main(String[] args){
        UserCreationTest();
        PlaylistTest();
        ShareTokenTest();
        PlaylistStateTest();
        PlaylistSearchTest();
        AuthTokenTest();
        SQLInjectionTest();
    }

    @Test(timeout = 100)
    public static void UserCreationTest(){
        String userName = "testUserName1";
        String password = "testPassword";
        String email = "testEmail@test.com";

        User user = User.CreateUser(userName, password, email);

        boolean actual = UserPassword.IsPasswordCorrect(userName, password);

        Assert.assertEquals(true, actual);

        SqlHelper helper = new SqlHelper();
        String deletionQuery = "DELETE FROM UserPasswords WHERE UserID = '" + user.ID + "'";
        String deletionQuery2 = "DELETE FROM Users WHERE ID = '" + user.ID + "'";

        helper.ExecuteQuery(deletionQuery);
        helper.ExecuteQuery(deletionQuery2);

        helper.closeConnection();
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
    public static void ShareTokenTest(){
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

        Assert.assertEquals(true, user.playlistList.size() == 1);

        Playlist returnedList = user.playlistList.get(0);

        Assert.assertEquals(true, returnedList.equals(playlist));
        Assert.assertEquals(true, returnedList.getSong(0).equals(song));
        Assert.assertEquals(true, returnedList.getSong(1).equals(song2));
    }

    @Test(timeout = 100)
    public static void PlaylistStateTest(){
        Playlist startList = new Playlist();
        startList.Name = "State playlist";

        String songFetch = "SELECT TOP 2 * FROM Songs";

        SqlHelper helper = new SqlHelper();

        ResultSet resultSet = helper.ExecuteQueryWithReturn(songFetch);

        Song song1 = new Song();
        Song song2 = new Song();
        try{
            resultSet.next();
            song1 = new Song(resultSet);
            resultSet.next();
            song2 = new Song(resultSet);
        }catch (SQLException e){
            System.err.println(e);
            Assert.fail();
        }

        User user = User.getUserByUserName("testUserName");

        user.FetchPlaylists();
        for(Playlist list : user.playlistList){
            list.delete();
        }

        startList.addSong(song1);
        startList.save(user);

        startList.clearSongs();
        startList.addSong(song2);
        startList.save(user);

        List<Playlist> previousPlaylists = startList.fetchPreviousStates();

        Assert.assertEquals(true, previousPlaylists.size() == 1);
        Assert.assertEquals(true, previousPlaylists.get(0).getNumSongs() == 1);
        Assert.assertEquals(true, previousPlaylists.get(0).getSong(0).title.equals(song1.title));
    }

    @Test(timeout = 100)
    public static void PlaylistSearchTest(){
        Playlist startList = new Playlist();
        startList.Name = "Search test";

        String songFetch = "SELECT TOP 2 * FROM Songs";

        SqlHelper helper = new SqlHelper();

        ResultSet resultSet = helper.ExecuteQueryWithReturn(songFetch);

        Song song1 = new Song();
        Song song2 = new Song();
        try{
            resultSet.next();
            song1 = new Song(resultSet);
            resultSet.next();
            song2 = new Song(resultSet);
        }catch (SQLException e){
            System.err.println(e);
            Assert.fail();
        }

        User user = User.getUserByUserName("testUserName");

        user.FetchPlaylists();
        for(Playlist list : user.playlistList){
            list.delete();
        }

        startList.addSong(song1);
        startList.addSong(song2);
        Song song3 = null;
        Song song4 = null;
        try {
            song3 = Spotify.findSong("I write sins not tragedies");
            song4 = Spotify.findSong("Ice ice baby");

            startList.addSong(song3);
            startList.addSong(song4);
        }catch(Exception e){
            System.err.println(e);
            Assert.fail();
        }

        Assert.assertEquals(true, song3.title.equalsIgnoreCase("I write sins not tragedies"));
        Assert.assertEquals(true, song4.title.equalsIgnoreCase("Ice ice baby"));

        Assert.assertEquals(true, startList.getSong(0).title.equals(song1.title));
        Assert.assertEquals(true, startList.getSong(1).title.equals(song2.title));
        Assert.assertEquals(true, startList.getSong(2).title.equals(song3.title));
        Assert.assertEquals(true, startList.getSong(3).title.equals(song4.title));
    }

    public static void AuthTokenTest(){
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement userFetch = helper.connection.prepareStatement("SELECT TOP 1 * FROM Users");

            ResultSet resultSet = userFetch.executeQuery();

            User user = null;
            while(resultSet.next()){
                user = new User(resultSet);
            }

            Assert.assertEquals(true, user.ID != 0);

            // Set Auth Token
            user.setAuthToken("abc123");

            // Check Auth Token
            Assert.assertEquals(true, user.isAuthTokenValid("abc123"));

        }catch (SQLException e){
            System.err.println(e);
            assert false;
        }
    }

    public static void SQLInjectionTest(){
        SqlHelper helper = new SqlHelper();

        // Make sure User Creation is working correctly
        UserCreationTest();

        try{
            // SQL Injection on userName
            User user = User.CreateUser(";DROP TABLE PlaylistHistorySongs;","whatever","whataver@whatever.com");

            // Send useless query to PlaylistHistorySongs
            PreparedStatement useless = helper.connection.prepareStatement("SELECT TOP 1 * FROM PlaylistHistorySongs");
            ResultSet dontCare = useless.executeQuery();

            // If query doesn't error then SQL Injection didn't work
        }catch (SQLException e){
            // SQLException thrown because the PlaylistHistorySongs table no longer exists
            System.err.println(e);
            System.err.println("\n               SQL TEST ERROR                    \n");

            try{
                // Send query to restore table
                PreparedStatement restorer = helper.connection.prepareStatement("CREATE TABLE dbo.PlaylistHistorySongs(\n" +
                        "\tID int PRIMARY KEY IDENTITY(1,1),\n" +
                        "\tPlaylistHistoryID int FOREIGN KEY REFERENCES PlaylistHistory(ID) not null,\n" +
                        "\tSongID int FOREIGN KEY REFERENCES Songs(ID) not null\n" +
                        ") ");
                restorer.execute();
            }catch (SQLException ruhRoh){
                System.err.println("\n               RUH ROH                  \n");
                System.err.println(ruhRoh);
            }

            // Can't forget to fail the test now can we
            assert false;
        }finally{
            helper.closeConnection();
        }
    }
}
