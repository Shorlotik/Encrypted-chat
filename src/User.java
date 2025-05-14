import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class User {
    private final String username;
    private final SecretKey encryptionKey;

    public User(String username, String password) {
        this.username = username;
        this.encryptionKey = generateEncryptionKey(password);
    }

    public String getUsername() {
        return username;
    }

    public SecretKey getEncryptionKey() {
        return encryptionKey;
    }

    private SecretKey generateEncryptionKey(String password) {
        // Пример генерации ключа шифрования с использованием хеша пароля
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            return new SecretKeySpec(hashedBytes, 0, 16, "AES");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}
