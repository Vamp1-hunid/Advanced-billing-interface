package lodgebilling.model;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/**
 * Checks usernames and passwords. Passwords are never kept as text: each one is
 * stored as a salted PBKDF2 hash, and a login is checked by hashing the attempt
 * the same way and comparing.
 */
public class AuthService {

    private static final int ITERATIONS = 120_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;

    private record Credential(byte[] salt, byte[] hash) { }

    private final Map<String, Credential> users = new HashMap<>();
    private final SecureRandom random = new SecureRandom();
    private String currentUser;

    public AuthService() {
        // Starting account. Change the password here (or add more users) before real use.
        addUser("admin", "lodge123".toCharArray());
    }

    public final void addUser(String username, char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        users.put(key(username), new Credential(salt, hash(password, salt)));
    }

    /** True and signed in if the username and password match; false otherwise. */
    public boolean login(String username, char[] password) {
        Credential stored = users.get(key(username));
        // Hash even for an unknown user so a wrong name and a wrong password take equally long
        byte[] salt = stored != null ? stored.salt() : new byte[SALT_BYTES];
        byte[] attempt = hash(password, salt);
        boolean ok = stored != null && MessageDigest.isEqual(stored.hash(), attempt);
        currentUser = ok ? username.trim() : null;
        return ok;
    }

    public void logout() {
        currentUser = null;
    }

    public String currentUser() {
        return currentUser;
    }

    private static String key(String username) {
        return username.trim().toLowerCase();
    }

    private static byte[] hash(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }
}
