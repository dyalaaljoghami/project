package coordinator;

import java.rmi.Remote;
import java.rmi.RemoteException;
import shared.AuthToken;
import shared.UserRole;


public interface CoordinatorService extends Remote {
    AuthToken login(String username, String password) throws RemoteException;
    boolean createUser(AuthToken token, String username, String password, String department, UserRole role) throws RemoteException;
    boolean writeFile(AuthToken token, String fileName, byte[] data) throws RemoteException;
    boolean updateFile(AuthToken token, String fileName, byte[] data) throws RemoteException;
    boolean deleteFile(AuthToken token, String fileName) throws RemoteException;
    byte[] readFile(AuthToken token, String fileName) throws RemoteException;
    boolean logout(AuthToken token) throws RemoteException;
    boolean validateToken(AuthToken token) throws RemoteException;
    UserRole getUserRole(AuthToken token) throws RemoteException;
    byte[] readAnyFile(AuthToken token, String department, String fileName) throws RemoteException;


}
