import javax.swing.plaf.nimbus.State;
import java.sql.*;

public class SqlHelper {

    private final String userName = "interlinkeddb";
    private final String password = "Tk5VQ73~0O?F";
    private final String url = "jdbc:sqlserver://den1.mssql2.gear.host;databaseName=interlinkeddb;user="+userName+";password="+password;
    private Connection connection;

    public SqlHelper(){
        // Opens a connection to the database
        try{
            connection = DriverManager.getConnection(url);
        }catch(SQLException e){
            // TODO
            System.err.println(e);
        }catch(Exception e){
            System.err.println(e);
        }
    }

    public ResultSet ExecuteQueryWithReturn(String query){
        // Sanitize Query
        query = query.replace(';', ' ');

        // Execute Query
        ResultSet results = null;
        try{
            Statement statement = connection.createStatement();
            results = statement.executeQuery(query);
        }catch(SQLException e){
            // TODO
            System.err.println(e);
        }
        return results;
    }

    public void ExecuteQuery(String query){
        // Sanitize Query
        query = query.replace(';', ' ');

        try{
            Statement statement = connection.createStatement();
            statement.execute(query);
        }catch(SQLException e) {
            // TODO
            System.err.println(e);
        }
    }

    public void closeConnection(){
        try{
            connection.close();
        }catch (SQLException e) {
            System.err.println(e);
        }
    }
}
