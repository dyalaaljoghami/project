package coordinator;

import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class Coordinator {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(2000);
            CoordinatorService coord = new CoordinatorImpl();
            Naming.rebind("rmi://localhost:2000/CoordinatorService", coord);
            System.out.println("Coordinator RMI server running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
