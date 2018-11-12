import java.sql.PreparedStatement;
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
            SqlHelper helper = new SqlHelper();

            try{
                PreparedStatement insertStatement = helper.connection.prepareStatement("INSERT INTO PlaylistSongs(PlaylistID, SongID) VALUES(?,?)");
                insertStatement.setInt(1, PlaylistID);
                insertStatement.setInt(2, SongID);

                insertStatement.execute();

                PreparedStatement findIDStatement = helper.connection.prepareStatement("SELECT ID FROM PlaylistSongs WHERE PlaylistID = ? AND SongID = ?");
                findIDStatement.setInt(1, PlaylistID);
                findIDStatement.setInt(2, SongID);

                ResultSet resultSet = findIDStatement.executeQuery();

                while(resultSet.next()){
                    ID = resultSet.getInt("ID");
                }
                helper.closeConnection();
                return true;
            }catch (SQLException e){
                System.err.println(e);
                return false;
            }
        }
    }

    public boolean delete(){
        SqlHelper helper = new SqlHelper();

        try{
            PreparedStatement deletionStatement = helper.connection.prepareStatement("DELETE FROM PlaylistSongs WHERE PlaylistID = ? AND SongID = ?");
            deletionStatement.setInt(1, PlaylistID);
            deletionStatement.setInt(2, SongID);

            deletionStatement.execute();
        }catch (SQLException e){
            System.err.println(e);
            return false;
        }

        helper.closeConnection();

        return true;
    }
}
