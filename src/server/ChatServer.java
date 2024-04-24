package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServer {
    private int port;
    private List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void execute() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Chat Server is listening on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();  // Accept new client connections
                System.out.println("New user connected");

                ClientHandler newUser = new ClientHandler(socket, this);
                clients.add(newUser);
                newUser.start();
            }
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void broadcast(String message, ClientHandler excludeUser) {
    	synchronized (clients) {
            for (ClientHandler aClient : clients) {
                if (aClient != excludeUser) {
                    aClient.sendMessage(message);
                }
            }
        }
    }

    public void removeUser(ClientHandler user, String userName) {
        boolean removed = clients.remove(user);
        if (removed) {
            System.out.println("The user " + userName + " disconnected");
            broadcast(userName + " has left the chat.", null);
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Syntax: java ChatServer <port-number>");
            return;
        }
        int port = Integer.parseInt(args[0]);
        ChatServer server = new ChatServer(port);
        server.execute();
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private ChatServer server;
    private PrintWriter writer;
    private BufferedReader reader;

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;

        try {
            OutputStream output = socket.getOutputStream();
            writer = new PrintWriter(output, true);

            InputStream input = socket.getInputStream();
            reader = new BufferedReader(new InputStreamReader(input));

            String userName = reader.readLine().trim(); // Read the user's name sent as the first message
            if (userName != null && !userName.isEmpty()) {
                server.broadcast(userName + " has joined the chat", this);
                this.setName(userName);  // Setting the thread name to the user's name for easier debugging
            }

            String clientMessage;
            // Continuously listen for messages from the client
            while ((clientMessage = reader.readLine()) != null) {
                server.broadcast("[" + userName + "]: " + clientMessage, this);
            }

            // Handle user disconnection
            server.removeUser(this, userName);
            socket.close();
            server.broadcast(userName + " has left the chat", null);

        } catch (IOException e) {
            System.out.println("Error in ClientHandler: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Socket close error: " + e.getMessage());
            }
        }
    }

    public void sendMessage(String message) {
        writer.println(message);
    }
}
