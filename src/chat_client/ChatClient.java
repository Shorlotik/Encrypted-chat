package chat_client;

import chat_network.TCPConnection;
import chat_network.TCPConnectionListener;
import chat_server.MessageHandler;
import chat_server.User;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ChatClient implements TCPConnectionListener {

    private TCPConnection connection;
    private User user;

    public static void main(String[] args) {
        new ChatClient().run();
    }

    private void run() {
        try {
            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

            // Ввод данных пользователя
            System.out.print("Введите имя пользователя: ");
            String username = console.readLine();
            System.out.print("Введите пароль: ");
            String password = console.readLine();

            // Создание пользователя
            user = new User(username, password);

            // Подключение к серверу
            connection = new TCPConnection(this, new Socket("localhost", 49676));

            // Авторизация
            connection.sendString("AUTH " + username + " " + password);

            // Запуск отдельного потока для чтения с консоли
            new Thread(() -> {
                try {
                    while (true) {
                        String msg = console.readLine();
                        if (msg.equalsIgnoreCase("/exit")) {
                            connection.disconnect();
                            break;
                        }
                        // Шифруем сообщение перед отправкой
                        String encrypted = MessageHandler.encrypt(msg, user.getEncryptionKey());
                        connection.sendString(encrypted);
                    }
                } catch (IOException e) {
                    System.out.println("Ошибка ввода: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка клиента: " + e);
        }
    }

    @Override
    public void onConnectionReady(TCPConnection tcpConnection) {
        System.out.println("Соединение установлено.");
    }

    @Override
    public void onReceiveString(TCPConnection tcpConnection, String message) {
        // Пытаемся расшифровать входящее сообщение
        try {
            String decrypted = MessageHandler.decrypt(message, user.getEncryptionKey());
            System.out.println(decrypted != null ? decrypted : "[Ошибка расшифровки] " + message);
        } catch (Exception e) {
            System.out.println("[Ошибка] Невозможно расшифровать сообщение: " + message);
        }
    }

    @Override
    public void onDisconnect(TCPConnection tcpConnection) {
        System.out.println("Соединение разорвано.");
    }

    @Override
    public void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("Ошибка TCP-соединения: " + e);
    }
}
