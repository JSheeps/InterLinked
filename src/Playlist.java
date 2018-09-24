import java.util.ArrayList;
import java.util.List;

class Playlist{

    private List<Song> playlist = new ArrayList<>();


    public void addSong(Song song){
        playlist.add(song);
    }

    public Song removeSong(int index){
        return playlist.remove(index);
    }

    public Song getSong(int index){
        return playlist.get(index);
    }
}