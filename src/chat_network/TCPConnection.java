package chat_network;

import java.io.*;
import java.net.Socket;

public class TCPConnection {

    private final Socket socket;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final TCPConnectionListener eventListener;
    private final Thread rxThread;

    public TCPConnection(TCPConnectionListener eventListener, Socket socket) throws IOException {
        this.socket = socket;
        this.eventListener = eventListener;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

        // Инициализация потока приёма сообщений
        this.rxThread = new Thread(() -> {
            try {
                eventListener.onConnectionReady(TCPConnection.this);
                while (!Thread.currentThread().isInterrupted()) {
                    String msg = in.readLine();
                    if (msg == null) break;
                    eventListener.onReceiveString(TCPConnection.this, msg);
                }
            } catch (IOException e) {
                eventListener.onException(TCPConnection.this, e);
            } finally {
                eventListener.onDisconnect(TCPConnection.this);
            }
        });
        rxThread.start();
    }

    /**
     * Отправка строки через соединение
     */
    public synchronized void sendString(String value) {
        try {
            out.write(value);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            eventListener.onException(this, e);
            disconnect();
        }
    }

    /**
     * Закрытие соединения
     */
    public synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventListener.onException(this, e);
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: " + socket.getInetAddress() + ":" + socket.getPort();
    }
}
