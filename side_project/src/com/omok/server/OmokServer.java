package com.omok.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Entry point for the Omok server.
 * Accepts client connections and spawns a new thread (ClientHandler) for each client.
 */
public class OmokServer {

    // Port number the server listens on
    private static final int PORT = 9999;

    public static void main(String[] args) {
        System.out.println("Omok server starting... (port: " + PORT + ")");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server ready. Waiting for clients...");

            // Infinite loop to continuously accept client connections
            while (true) {
                // accept() blocks here until a client connects
                Socket clientSocket = serverSocket.accept();

                // Spawn a new thread for each connected client
                // Main thread immediately returns to waiting for the next client
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}