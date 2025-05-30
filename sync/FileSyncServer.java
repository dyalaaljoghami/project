package sync;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;

public class FileSyncServer {
    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(args[0]);
        ServerSocket server = new ServerSocket(port);
        System.out.println("Listening for sync on port " + port);
        while (true) {
            Socket socket = server.accept();
            new Thread(() -> {
                try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    String dept = in.readUTF();
                    String file = in.readUTF();
                    int len = in.readInt();
                    byte[] data = new byte[len];
                    in.readFully(data);
                    Path path = Paths.get("files", dept, file);
                    Files.createDirectories(path.getParent());
                    Files.write(path, data);
                    System.out.println("Received: " + dept + "/" + file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
