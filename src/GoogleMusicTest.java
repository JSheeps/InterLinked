import java.util.ArrayList;
import java.util.List;
public class GoogleMusicTest {
    public static void main(String[] args){
     try {
      ArrayList<String> importedPlaylistsID = new ArrayList<String>();
      //required login info
      String username = "cs307acct@gmail.com";
      String password = "p4S5WoRd";
      String imeinumber = "352593081955740";

      //login
      String auth = GoogleMusic.Login(username, password, imeinumber);
      System.out.println("Auth token: " + auth);

      //getplaylists
      List<Playlist> playlists;
      playlists = GoogleMusic.getPlaylists(auth);
      System.out.println("User's Playlists are: ");
      for (int i = 0; i < playlists.size(); i++) {
       System.out.println(playlists.get(i).Name + " " + playlists.get(i).googleId);
       importedPlaylistsID.add(playlists.get(i).googleId);
      }

      System.out.println("\n");

      //find song test
      Song s = new Song();
      s = GoogleMusic.findSong(("Havana"));
      System.out.println("Searching for song Havana");
      System.out.println("Artist: " + s.artist + "\nSong title: " + s.title + "\nAlbum: " + s.album + "\ngoogle id: " + s.googleId);

      //import test
      List<Song> importedSongs;
      importedSongs = GoogleMusic.importPlaylist(auth, importedPlaylistsID.get(0));
      System.out.println("\nImported playlist name: " +playlists.get(0).Name+" id: " + importedPlaylistsID.get(0) +"\n");
      System.out.println("Songs include: "+importedSongs+"\n");

      //export test
      //exporting same playlist that was imported, should basically clone it
      Playlist newList = new Playlist();
      newList.Name = "playlist test";
      newList.addSong(s);

      List<Song> failedSongs = GoogleMusic.exportPlaylist(auth, newList);

      //get playlists again
      playlists.clear();
      playlists = GoogleMusic.getPlaylists(auth);
      System.out.println("User's Playlists after exporting are: ");
      for (int i = 0; i < playlists.size(); i++) {
       System.out.println(playlists.get(i).Name + " " + playlists.get(i).googleId);
      }

      System.out.println("\n");


     }
     catch (Exception e){
      e.printStackTrace();
     }
    }

}
