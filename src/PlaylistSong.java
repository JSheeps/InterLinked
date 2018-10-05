import java.sql.ResultSet;
import java.sql.SQLException;

public class PlaylistSong {

    public int ID;
    public int PlaylistID;
    public int SongID;

    public PlaylistSong(int playlistID, int songID){
        PlaylistID = playlistID;
        SongID = songID;
    }

    public boolean save(){
        if(ID != 0){
            // Already in DB
            return true;
        }else{
            String insertQuery = "INSERT INTO PlaylistSongs(PlaylistID, SongID) VALUES(" + PlaylistID + ", " + SongID + ")";
            SqlHelper helper = new SqlHelper();
            helper.ExecuteQuery(insertQuery);

            // Find ID of thing we just inserted
            String findIDQuery = "SELECT ID FROM PlaylistSongs WHERE PlaylistID = " + PlaylistID + " AND SongID = " + SongID;
            ResultSet resultSet = helper.ExecuteQueryWithReturn(findIDQuery);

            try{
                ID = resultSet.getInt("ID");
                return true;
            }catch (SQLException e){
                // TODO
                return false;
            }
        }
    }

    public boolean delete(){
        String deletionQuery = "DELETE FROM PlaylistSongs WHERE PlaylistID = " + PlaylistID + " AND SongID = " + SongID;
        SqlHelper helper = new SqlHelper();

        helper.ExecuteQuery(deletionQuery);

        return true;
    }
}
