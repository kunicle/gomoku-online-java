package com.omok.client;

import com.omok.common.MessageProtocol;

import java.io.*;
import java.net.Socket;

/**
 * Manages the socket connection to the server.
 * Receives server messages on a separate thread to avoid blocking the UI thread.
 */
public class NetworkClient implements Runnable {

    private static final String HOST = "localhost";
    private static final int PORT = 9999;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Callback handler that defines how to process received messages
    // Injected by LobbyPanel, GamePanel, etc. via MainFrame
    private MessageHandler messageHandler;

    private boolean connected = false;

    // Callback interface for message handling
    // Allows each panel to define its own message processing logic
    public interface MessageHandler {
        void onMessage(String message);
    }

    public NetworkClient(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    // Connect to server
    public boolean connect() {
        try {
            socket = new Socket(HOST, PORT);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            connected = true;

            // Start receive thread - runs separately to avoid blocking the UI thread
            Thread receiveThread = new Thread(this);
            receiveThread.setDaemon(true); // Terminates when main program exits
            receiveThread.start();

            System.out.println("Server connection successful");
            return true;

        } catch (IOException e) {
            System.out.println("Server connection failed: " + e.getMessage());
            return false;
        }
    }

    // Receive thread - continuously reads messages from server
    @Override
    public void run() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from server: " + message);
                // Dispatch to UI thread safely via SwingUtilities
                final String msg = message;
                javax.swing.SwingUtilities.invokeLater(() -> messageHandler.onMessage(msg));
            }
        } catch (IOException e) {
            System.out.println("Server connection lost: " + e.getMessage());
        } finally {
            connected = false;
        }
    }

    // Send message to server
    public void send(String message) {
        if (connected && out != null) {
            System.out.println("Sent to server: " + message);
            out.println(message);
        }
    }

    // Close connection
    public void disconnect() {
        try {
            connected = false;
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Disconnect error: " + e.getMessage());
        }
    }

    public boolean isConnected() { return connected; }
}