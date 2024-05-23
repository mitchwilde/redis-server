import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.lang.System.*;

public class Main {
    private static final int PORT = 6379;
    private Map<String, String> values;

    public static void main() {
        List<Future<?>> futureList = new ArrayList<>();
        // Thread pool for handling clients
        try (ExecutorService executorService = Executors.newCachedThreadPool();
             ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            out.println(STR."Redis server started on port \{PORT}");
            while (true) { // Continuously accept connections
                for (Future<?> future: futureList) {
                    if (future.isDone()) {
                        var thread = (Thread) future.get();
                        out.println(STR."\{thread.getName()}task is complete: \{future.isDone()}");
                    }
                }
                Socket clientSocket = serverSocket.accept();
                clientSocket.setKeepAlive(true);
                // Execute a task using a thread from the thread pool for each client connection
                // ** uses shorthand lambda expression which implements Runnable
                Future<?> future = executorService.submit(() -> handleClient(clientSocket));
                futureList.add(future);
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            err.println(STR."IOException in main: \{e.getMessage()}");
        }
    }


    private static void handleClient(Socket clientSocket) {
        Main main = new Main();
        main.values = new HashMap<>();
        try {
            while (clientSocket.isConnected() && !clientSocket.isClosed()) {
                byte[] input = new byte[1024];
                int bytesRead = clientSocket.getInputStream().read(input);
                if (bytesRead > 0) {
                    String request = new String(input, 0, bytesRead).trim();
                    // Parse the command and handle accordingly
                    String[] parts = request.split("\r\n");
                    Parser parser = new Parser();
                    StringBuilder response = new StringBuilder();
                    if (parts.length <= 1) {
                        response.append("-ERR invalid request\r\n");
                    } else if (parts.length == 2) {
                        response.append(parser.parseBlobString(parts));
                    } else {
                        String command = parts[2];
                        switch (command) {
                            case "PING" -> response.append("+PONG\r\n");
                            case "ECHO" -> {
                                // Extract the data from the request
                                String data = parts[4]; // Remove the leading '$'

                                // Append to the response
                                response.append(STR."$\{data.length()}\r\n\{data}\r\n");
                            }
                            case "GET" -> {
                                String value = main.values.get(parts[4]);
                                response.append(STR."$\{value.length()}\r\n\{value}\r\n");
                            }
                            case "SET" -> {
                                main.values.put(parts[4], parts[6]);
                                response.append("+OK\r\n");
                            }
                            default ->
                                // Handle other commands or send an error response
                                    response.append("-ERR unknown command\r\n");
                        }
                    }
                    clientSocket.getOutputStream().write(response.toString().getBytes());
                }
            }
        } catch (IOException e) {
            err.println(STR."Error handling client: \{e.getMessage()}");
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                err.println(STR."Error closing client socket: \{e.getMessage()}");
            }
        }
    }
}
