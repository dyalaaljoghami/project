package sync;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;

public class FileSyncClient {
    public static void main(String[] args) throws IOException {
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String dept = args[2];
        String file = args[3];
        Path path = Paths.get("files", dept, file);
        byte[] data = Files.readAllBytes(path);
        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.writeUTF(dept);
            out.writeUTF(file);
            out.writeInt(data.length);
            out.write(data);
            System.out.println("Sent: " + dept + "/" + file + " to " + host + ":" + port);
        }
    }
}
