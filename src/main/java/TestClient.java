import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.Buffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.StringTemplate.STR;

public class TestClient {
    public static void main(String... args) {
        try (ExecutorService executorService = Executors.newCachedThreadPool()) {
            int numberOfThreads = 10;
            for (int i = 0; i < numberOfThreads; i++) {
                executorService.submit(TestClient::run);
            }
        }
    }

    private static void sendRequestToServer(Socket socket) throws IOException {
        socket.getOutputStream().write("*1\r\n$4\r\nPING\r\n".getBytes());
        var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println(STR."response: \{reader.readLine()}");
    }

    private static void run() {
        try {
            Socket socket = new Socket("localhost", 6379);
            sendRequestToServer(socket);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
