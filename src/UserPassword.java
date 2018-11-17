import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPassword {

	public int UserID;
	public String Salt;
	public String SaltedPassword;

	public UserPassword(int userID, String salt, String saltedPassword){
		UserID = userID;
		Salt = salt;
		SaltedPassword = saltedPassword;
	}

	public static boolean CreateUserPassword(int userID, String Password){
		// Generate Salt
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[50];
		random.nextBytes(salt);

		// Append salt to Password
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < salt.length; ++i) {
            sb.append(Integer.toHexString((salt[i] & 0xFF) | 0x100).substring(1, 3));
        }
        String saltString = sb.toString();
		String saltPlusPass = saltString + Password;

		// Hash Salt + Password
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] saltedHash = digest.digest(saltPlusPass.getBytes(StandardCharsets.UTF_8));
            sb = new StringBuilder();
            for (int i = 0; i < saltedHash.length; ++i) {
                sb.append(Integer.toHexString((saltedHash[i] & 0xFF) | 0x100).substring(1, 3));
            }

			UserPassword up = new UserPassword(userID, saltString, sb.toString());

			// Save to DB
			SqlHelper helper = new SqlHelper();

			try{
				PreparedStatement insertionStatement = helper.connection.prepareStatement("INSERT INTO UserPasswords(UserID, Salt, SaltedPassword) VALUES(?,?,?)");
				insertionStatement.setInt(1, up.UserID);
				insertionStatement.setString(2, up.Salt);
				insertionStatement.setString(3, up.SaltedPassword);

				insertionStatement.execute();
			}catch (SQLException e){
				System.err.println(e);
				return false;
			}

			helper.closeConnection();
		}catch(NoSuchAlgorithmException e) {
            System.err.println(e);
			return false;
		}

		return true;
	}

	// Returns true on successful login, false on unsuccessful login
	public static boolean IsPasswordCorrect(String userName, String password){
        // Get UserPassword object associated with userName
		SqlHelper helper = new SqlHelper();
		String Salt = "";
		String SaltedPassword = "";

		try{
			PreparedStatement upFetchStatement = helper.connection.prepareStatement("SELECT UserPasswords.* FROM UserPasswords JOIN Users on Users.ID = UserPasswords.UserID WHERE Users.UserName = ?");
			upFetchStatement.setString(1, userName);

			ResultSet results = upFetchStatement.executeQuery();

			while(results.next()){
				Salt = results.getString("Salt");
				SaltedPassword = results.getString("SaltedPassword");
			}
			helper.closeConnection();
		}catch(SQLException e){
			System.err.println(e);
			return false;
		}

		// Append salt to Password
		String saltPlusPass = Salt + password;

		// Hash Salt + Password
		String userInputtedPasswordHashed = "";
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] saltedHash = digest.digest(saltPlusPass.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < saltedHash.length; ++i) {
                sb.append(Integer.toHexString((saltedHash[i] & 0xFF) | 0x100).substring(1, 3));
            }

			// Check to see if inputted password hashed == stored salted password
			userInputtedPasswordHashed = sb.toString();

		}catch(NoSuchAlgorithmException e) {
			// This will never happen, but java is making me do this
            System.err.println(e);
		}

		if(userInputtedPasswordHashed.equals(SaltedPassword)){
			return true;
		}else{
			return false;
		}
	}
}
