import javax.swing.plaf.nimbus.State;
import java.sql.*;

public class SqlHelper {

    private final String userName = "dummyUserName";
    private final String password = "dummyPass";
    private final String url = "dummyURL";
    private Connection connection;

    public SqlHelper(){
        // Opens a connection to the database
        try{
            connection = DriverManager.getConnection(url, userName, password);
        }catch(SQLException e){
            // TODO
        }
    }

    public ResultSet ExecuteQuery(String query){
        // Sanitize Query
        query = query.replace(';', ' ');

        // Execute Query
        ResultSet results = null;
        try{
            Statement statement = connection.createStatement();
            results = statement.executeQuery(query);
        }catch(SQLException e){
            // TODO
        }
        return results;
    }
}
