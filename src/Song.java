
public class Song{

    String artist;
    String title;
    String album;

    // Song diration in milliseconds
    int duration;

    boolean explicit;
    String spotfyID;
    String spitufyURI;

    enum OriginHostName{
        AMAZON,
        ITUNES,
        SPOTIFY,
        YOUTUBE
    }
}