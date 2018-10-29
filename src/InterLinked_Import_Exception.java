public class InterLinked_Import_Exception extends Exception{

    private String[] missingSongs = {};
    private Playlist playlist;

    public InterLinked_Import_Exception(String [] missingSongs, Playlist playlist){
        this.missingSongs = missingSongs;
        this.playlist = playlist;
    }

    public String[] getMissingSongs(){
        String[] temp = missingSongs;
        return temp;
    }
}
