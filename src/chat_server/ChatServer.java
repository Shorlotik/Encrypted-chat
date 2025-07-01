package chat_server;

import chat_network.TCPConnection;
import chat_network.TCPConnectionListener;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.*;

public class ChatServer implements TCPConnectionListener {

    public static void main(String[] args){
        new ChatServer();
    }

    // Список всех активных соединений
    private final List<TCPConnection> connections = new ArrayList<>();

    // Ассоциация соединения с пользователем
    private final Map<TCPConnection, User> authenticatedUsers = new HashMap<>();

    // Хранилище зарегистрированных пользователей (в памяти)
    private final Map<String, User> registeredUsers = new HashMap<>();

    private ChatServer(){
        System.out.println("Server running...");
        try(ServerSocket serverSocket = new ServerSocket(49676)){
            while (true){
                try{
                    new TCPConnection(this, serverSocket.accept());
                } catch (IOException e){
                    System.out.println("TCPConnection exception: " + e);
                }
            }
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void onConnectionReady(TCPConnection tcpConnection) {
        connections.add(tcpConnection);
        tcpConnection.sendString("Welcome! Please authenticate using: AUTH <username> <password>");
    }

    @Override
    public synchronized void onReceiveString(TCPConnection tcpConnection, String message) {
        // Проверяем, авторизован ли пользователь
        if (!authenticatedUsers.containsKey(tcpConnection)) {
            // Ожидаем команду AUTH
            if (message.startsWith("AUTH ")) {
                String[] tokens = message.split(" ", 3);
                if (tokens.length < 3) {
                    tcpConnection.sendString("Invalid AUTH command. Usage: AUTH <username> <password>");
                    return;
                }
                String username = tokens[1];
                String password = tokens[2];

                User user = registeredUsers.get(username);
                if (user == null) {
                    // Новый пользователь
                    user = new User(username, password);
                    registeredUsers.put(username, user);
                    tcpConnection.sendString("New user registered: " + username);
                } else {
                    // Проверка по ключу
                    SecretKey inputKey = user.generateEncryptionKey(password);
                    if (!Arrays.equals(inputKey.getEncoded(), user.getEncryptionKey().getEncoded())) {
                        tcpConnection.sendString("Authentication failed: incorrect password.");
                        return;
                    }
                    tcpConnection.sendString("Authentication successful: " + username);
                }

                authenticatedUsers.put(tcpConnection, user);
                sendToAllConnections("User joined: " + username);
            } else {
                tcpConnection.sendString("You must authenticate first using: AUTH <username> <password>");
            }
            return;
        }

        // Получаем пользователя
        User sender = authenticatedUsers.get(tcpConnection);
        String encrypted = MessageHandler.encrypt(message, sender.getEncryptionKey());

        // Шифруем и пересылаем всем, включая отправителя
        for (TCPConnection conn : connections) {
            User receiver = authenticatedUsers.get(conn);
            if (receiver != null) {
                String decrypted = MessageHandler.decrypt(encrypted, receiver.getEncryptionKey());
                conn.sendString(sender.getUsername() + ": " + decrypted);
            }
        }
    }

    @Override
    public synchronized void onDisconnect(TCPConnection tcpConnection) {
        connections.remove(tcpConnection);
        User user = authenticatedUsers.remove(tcpConnection);
        if (user != null) {
            sendToAllConnections("User disconnected: " + user.getUsername());
        }
    }

    @Override
    public synchronized void onException(TCPConnection tcpConnection, Exception e) {
        System.out.println("TCPConnection exception: " + e);
    }

    private void sendToAllConnections(String value){
        System.out.println(value);
        for (TCPConnection conn : connections){
            conn.sendString(value);
        }
    }
}
