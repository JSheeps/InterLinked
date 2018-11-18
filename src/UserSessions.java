import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.*;

// List of current sessions and their corresponding users
public class UserSessions extends HashMap<String, User> {
    Debug debug;
    private HashMap<String, ResetInfo> passwordResetSessions = new HashMap<>();
    private String path;

    public UserSessions(Debug d) {
        this(d, "tokens.txt");
    }

    public UserSessions(Debug d, String path) {
        this.debug = d;
        this.path = path;

        List<String> strings = null;
        try {
            strings = Files.readAllLines(Paths.get(path));
        } catch (IOException e) {
            if (e instanceof java.nio.file.NoSuchFileException) {
                System.out.println("Could not find " + path);
            } else
                e.printStackTrace();
        }

        if (strings != null) {
            for(String line : strings) {
                String[] columns = line.split("\\s");
                this.put(columns[0],User.getUserByUserName(columns[1]));
            }
        }
    }

    public String generateAuthToken(User user) {
        SecureRandom random = new SecureRandom();
        String token;
        do {
            token = generateToken(random);
        } while(this.containsKey(token));

        debug.log("Created token: " + token);

        this.put(token, user);
        String save = token + "\t" + user.userName + "\n";

        Path tokensPath = Paths.get(".", path);
        try {
            Files.write(tokensPath, save.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            try {
                Files.write(tokensPath, save.getBytes(), StandardOpenOption.CREATE);
            } catch (IOException f) {
                e.printStackTrace();
                f.printStackTrace();
            }
        }

        return token;
    }

    public String generateResetToken(User user) {
        SecureRandom random = new SecureRandom();
        String token;
        do {
            token = generateToken(random);
        } while (passwordResetSessions.containsKey(token));

        passwordResetSessions.put(token, new ResetInfo(user));
        return token;
    }

    public User getUserWithResetToken(String token) throws TokenExpiredException {
        ResetInfo r =  passwordResetSessions.get(token);
        if (r == null)
            return null;

        if (!Calendar.getInstance().before(r.expiration))
            throw new TokenExpiredException("Token has expired");

        return r.user;
    }

    public void deleteResetSession(String token) { passwordResetSessions.remove(token); }

    public void save() throws IOException { this.save(path); }
    public void save(String path) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (Map.Entry<String, User> entry : this.entrySet()) {
                String line = entry.getKey() + "\t" + entry.getValue().userName;
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private final static String s = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static String generateToken(Random random) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            stringBuilder.append(s.charAt(random.nextInt(s.length())));
        }

        return stringBuilder.toString();
    }
}

class ResetInfo {
    Calendar expiration;
    User user;
    ResetInfo(User user, Calendar expiration) {
        init(user, expiration);
    }

    ResetInfo(User user) {
        Calendar expiration = Calendar.getInstance();
        expiration.add(Calendar.HOUR, 1);
        init(user, expiration);
    }

    private void init(User user, Calendar expiration) {
        this.user = user;
        this.expiration = expiration;
    }
}

class TokenExpiredException extends Exception {
    TokenExpiredException(String s) {
        super(s);
    }
}