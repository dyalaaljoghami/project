package node;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface FileNode extends Remote {
    byte[] readFile(String fileName) throws RemoteException;

    boolean writeFile(String fileName, byte[] data) throws RemoteException;

    void updateFile(String fileName, byte[] data) throws RemoteException;

    void deleteFile(String fileName) throws RemoteException;

    boolean isAlive() throws RemoteException;

    boolean fileExists(String fileName) throws RemoteException;
    List<String> getAllFiles() throws RemoteException;

}