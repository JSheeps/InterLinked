import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PlaylistTest {

    @Test
    public void testAddRemoveSong(){
         Playlist playlist1 = buildPlaylist();
         Song song = new Song();
         song.artist = "Rihanna";
         song.title = "Umbrella";
         song.explicit = false;

         int i = playlist1.getSize();
         playlist1.addSong(song);

         assert playlist1.removeSong(i).equals(song) : "AddRemove Error";
     }

    @Test
    public void testMergePlaylists() {
        Playlist playlist1 = buildPlaylist();
        Playlist playlist2 = buildPlaylist();

        Song dupsong = new Song();
        dupsong.title = "Dupsong";
        dupsong.artist = "Dupartist";
        dupsong.album = "Dupalbum";

        playlist1.addSong(dupsong);
        playlist2.addSong(dupsong);

        Playlist newPlaylist = playlist1.merge(playlist2);

        for(int i = 0; i < playlist1.getSize(); i++){
            Song song = playlist1.getSong(i);
            assert newPlaylist.contains(song) : "Missing song from playlist1: " + song;
        }
        for(int i = 0; i < playlist2.getSize(); i++){
            Song song = playlist2.getSong(i);
            assert newPlaylist.contains(song) : "Missing song from playlist2: " + song;
        }

        Set<Song> set = new HashSet<>(newPlaylist.getArrayList());
        assert set.size() == newPlaylist.getSize() : "Duplicate in new playlist";
    }

    private Playlist buildPlaylist() {
        Playlist playlist = new Playlist();
        Random random = new Random();

        for (int i = 0; i < random.nextInt(500); i++) {
            Song song = new Song();
            song.artist = "artist" + random.nextInt();
            song.title = "title" + random.nextInt();

            playlist.addSong(song);
        }

        return playlist;
    }
}
