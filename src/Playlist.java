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

    public int getSize(){ return playlist.size(); }

    public List<Song> getArrayList() {
        return new ArrayList<>(playlist);
    }

    public boolean contains(Song song){ return playlist.contains(song); }

    // Returns a new playlist object that contains songs from both playlists with no duplicates
    // Compares using overridden equals in song object.

    public Playlist merge(Playlist playlist1) {

        Playlist newPlaylist = new Playlist();
        newPlaylist.playlist.addAll(this.playlist);

        for (Song song : playlist1.playlist) {
            if (!newPlaylist.playlist.contains(song))
                newPlaylist.addSong(song);
        }

        return newPlaylist;
    }

}