import com.sun.xml.internal.ws.commons.xmlutil.Converter;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

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
			String insertion = "INSERT INTO UserPasswords(UserID, Salt, SaltedPassword) VALUES(" + up.UserID + ", '" + up.Salt + "', '" + up.SaltedPassword + "')";
			helper.ExecuteQuery(insertion);
			helper.closeConnection();
		}catch(NoSuchAlgorithmException e) {
			// TODO
            System.err.println(e);
			return false;
		}

		return true;
	}

	// Returns true on successful login, false on unsuccessful login
	public static boolean IsPasswordCorrect(String userName, String password){
        // Get UserPassword object associated with userName
		String upFetch = "SELECT UserPasswords.* FROM UserPasswords JOIN Users on Users.ID = UserPasswords.UserID WHERE Users.UserName = '" + userName +"'";

		SqlHelper helper = new SqlHelper();
		ResultSet results = helper.ExecuteQueryWithReturn(upFetch);

		String Salt = "";
		String SaltedPassword = "";

		try{
			while(results.next()){
				Salt = results.getString("Salt");
				SaltedPassword = results.getString("SaltedPassword");
			}
			helper.closeConnection();
		}catch(SQLException e){
			// TODO
			System.err.println(e);
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
