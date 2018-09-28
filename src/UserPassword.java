import com.intellij.openapi.util.Pass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

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

		// TODO save to db
	}

	private int UserID;
	private String Salt;
	private String SaltedPassword;
	
	// Returns true on successful login, false on unsuccessful login
	public static boolean IsPasswordCorrect(String userName, String password){
        // TODO get UserPassword object associated with userName

		// Dummy values
		String Salt = "saltyboi";
		String SaltedPassword = "evensaltierboi";

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
