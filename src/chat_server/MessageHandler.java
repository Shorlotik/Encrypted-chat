package chat_server;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

public class MessageHandler {

    // Размер IV (AES блок = 16 байт)
    private static final int IV_SIZE = 16;

    /**
     * Шифруем сообщение, добавляя IV к результату (Base64(IV + encrypted))
     */
    public static String encrypt(String message, SecretKey key) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            // Генерируем случайный IV
            byte[] iv = new byte[IV_SIZE];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);

            byte[] encrypted = cipher.doFinal(message.getBytes());

            // Склеиваем IV и шифротекст
            byte[] combined = new byte[IV_SIZE + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE);
            System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Расшифровываем сообщение: извлекаем IV, затем дешифруем
     */
    public static String decrypt(String encryptedMessage, SecretKey key) {
        try {
            byte[] combined = Base64.getDecoder().decode(encryptedMessage);

            // Извлекаем IV и данные
            byte[] iv = new byte[IV_SIZE];
            byte[] encrypted = new byte[combined.length - IV_SIZE];

            System.arraycopy(combined, 0, iv, 0, IV_SIZE);
            System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return "[Ошибка расшифровки]";
        }
    }
}
