package chat_server;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

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

    /**
     * Генерация AES-ключа на основе пароля (используется только SHA-256 hash)
     */
    public SecretKey generateEncryptionKey(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashed = md.digest(password.getBytes());

            // Используем первые 16 байт хеша для ключа AES-128
            return new SecretKeySpec(Arrays.copyOf(hashed, 16), "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }

    /**
     * Проверка пароля (генерируем ключ и сравниваем с текущим)
     */
    public boolean isPasswordCorrect(String password) {
        SecretKey inputKey = generateEncryptionKey(password);
        return Arrays.equals(inputKey.getEncoded(), encryptionKey.getEncoded());
    }
}
