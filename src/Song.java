import java.util.Objects;

public class Song{

    String artist;
    String title;
    String album;

    // Song duration in milliseconds
    int duration;

    boolean explicit;
    String spotfyID;
    String spotifyURI;

    enum OriginHostName{
        AMAZON,
        ITUNES,
        SPOTIFY,
        YOUTUBE
    }

    // Only compares song title and artist

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return Objects.equals(artist, song.artist) &&
                Objects.equals(title, song.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(artist, title);
    }

    @Override
    public String toString() {
        return "Song{" +
                "artist='" + artist + '\'' +
                ", title='" + title + '\'' +
                ", album='" + album + '\'' +
                ", duration=" + duration +
                ", explicit=" + explicit +
                ", spotfyID='" + spotfyID + '\'' +
                ", spotifyURI='" + spotifyURI + '\'' +
                '}';
    }
}