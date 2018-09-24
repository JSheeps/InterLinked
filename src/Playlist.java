import java.util.List;
import java.util.ArrayList;

class Playlist{

    private List<Song> playlist = new ArrayList<Song>();


    public void addSong(Song song){
        playlist.add(song);
    }

    public Song removeSong(int index){
        return playlist.remove(index);
    }

    public getSong(int index){
        return playlist.get(index);
    }
}