import org.junit.Assert;
import org.junit.Test;

public class UnitTests {

    public static void main(String[] args){
        UserCreationTest1();
    }

    @Test(timeout = 100)
    public static void UserCreationTest1(){
        String userName = "testUserName";
        String password = "testPassword";
        String email = "testEmail@test.com";

        User user = User.CreateUser(userName, password, email);

        boolean actual = UserPassword.IsPasswordCorrect(userName, password);

        boolean expected = true;

        SqlHelper helper = new SqlHelper();
        String deletionQuery = "DELETE FROM UserPasswords WHERE UserID = " + "'" + user.ID + "'";
        String deletionQuery2 = "DELETE FROM Users WHERE ID = " + "'" + user.ID + "'";

        helper.ExecuteQuery(deletionQuery);
        helper.ExecuteQuery(deletionQuery2);

        Assert.assertEquals(expected, actual);
    }
}
