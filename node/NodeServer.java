package node;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class NodeServer {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            FileNodeImpl node = new FileNodeImpl();
            Registry registry = LocateRegistry.createRegistry(port);
            registry.rebind("FileNodeService", node);
            System.out.println("Node started on port " + port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
