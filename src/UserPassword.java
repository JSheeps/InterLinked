import com.intellij.openapi.util.Pass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPassword {

	public UserPassword(int userID, String Password){
		this.UserID = userID;

		// Generate Salt
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[50];
		random.nextBytes(salt);

		// Append salt to Password
		String saltString = new String(salt);
		String saltPlusPass = saltString + Password;

		// Hash Salt + Password
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] saltedHash = digest.digest(saltPlusPass.getBytes());

			this.Salt = saltString;
			this.SaltedPassword = new String(saltedHash);
		}catch(NoSuchAlgorithmException e) {
			// This will never happen, but java is making me do this
		}

		this.UserID = userID;

		// Save to DB
		SqlHelper helper = new SqlHelper();
		String insertion = "INSERT INTO UserPasswords(UserID, Salt, SaltedPassword) VALUES(" + this.UserID + ", " + this.Salt + ", " + this.SaltedPassword + ")";
		helper.ExecuteQuery(insertion);
	}

	private int UserID;
	private String Salt;
	private String SaltedPassword;
	
	// Returns true on successful login, false on unsuccessful login
	public static boolean IsPasswordCorrect(String userName, String password){
        // Get UserPassword object associated with userName
		String upFetch = "SELECT * FROM UserPasswords JOIN UserPasswords on Users.ID = UserPasswords.UserID WHERE Users.UserName = " + userName;
		SqlHelper helper = new SqlHelper();

		ResultSet results = helper.ExecuteQuery(upFetch);

		// Dummy values
		String Salt = "";
		String SaltedPassword = "";

		try{
			Salt = results.getString("Salt");
			SaltedPassword = results.getString("SaltedPassword");
		}catch(SQLException e){
			// TODO
		}

		// Append salt to Password
		String saltPlusPass = Salt + password;

		// Hash Salt + Password
		String userInputtedPasswordHashed = "";
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] saltedHash = digest.digest(saltPlusPass.getBytes());

			// Check to see if inputted password hashed == stored salted password
			userInputtedPasswordHashed = new String(saltedHash);

		}catch(NoSuchAlgorithmException e) {
			// This will never happen, but java is making me do this
		}

		if(userInputtedPasswordHashed.equals(SaltedPassword)){
			return true;
		}else{
			return false;
		}
	}
}
