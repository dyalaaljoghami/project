package coordinator;

import node.FileNode;
import shared.*;

import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CoordinatorImpl extends UnicastRemoteObject implements CoordinatorService {
    private static final String USERS_FILE = "users.txt";
    private static Map<String, User> users = new HashMap<>();
    private static Map<String, AuthToken> sessions = new HashMap<>();
    private static Map<String, String> departmentToNode = new HashMap<>();
    private Map<String, String> fileLocations = new HashMap<>();
    private final Map<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();
    private final LoadBalancer loadBalancer = new RoundRobinLoadBalancer(NODE_URIS);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;
    private static final String[] NODE_URIS = {
            "rmi://localhost:2001/FileNodeService",
            "rmi://localhost:2002/FileNodeService",
            "rmi://localhost:2003/FileNodeService"
    };
    public CoordinatorImpl() throws RemoteException {
        super();
        loadUsers();
        System.out.println("Coordinator started and ready.");
    }

    private void loadUsers() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    String username = parts[0];
                    String password = parts[1];
                    String department = parts[2];
                    UserRole role = UserRole.valueOf(parts[3]);
                    users.put(username, new User(username, password, department, role));
                }
            }
        } catch (IOException e) {
            System.out.println("No users file found. Starting fresh.");
        }
    }

    private void saveUserToFile(User user) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(USERS_FILE, true))) {
            writer.write(user.username + "," + user.password + "," + user.department + "," + user.role);
            writer.newLine();
        } catch (IOException e) {
            System.out.println("Failed to save user: " + user.username);
        }
    }

    @Override
    public UserRole getUserRole(AuthToken token) throws RemoteException {
        User user = getUserByToken(token);
        if (user == null) return null;
        return user.role;
    }
    private boolean isNodeAlive(String nodeUri) {
        try {
            FileNode node = getNode(nodeUri);
            return node.isAlive();
        } catch (Exception e) {
            return false;
        }
    }

    private String findAvailableNode() throws RemoteException {
        for (String nodeUri : NODE_URIS) {
            if (isNodeAlive(nodeUri)) {
                return nodeUri;
            }
        }
        throw new RemoteException("No available nodes");
    }

    private String findNodeWithFile(String filepath) throws RemoteException {
        String Node = getRandomNodeURI();
        fileLocations.put(filepath,Node);
        if (Node != null && isNodeAlive(Node)) {
            return Node;
        }

        for (String nodeUri : NODE_URIS) {
            if (!nodeUri.equals(Node) && isNodeAlive(nodeUri)) {
                try {
                    FileNode node = getNode(nodeUri);
                    if (node.fileExists(filepath)) {
                        fileLocations.put(filepath, nodeUri);
                        return nodeUri;
                    }
                } catch (Exception e) {
                    continue;
                }
            }
        }
        throw new RemoteException("File not found in any available node");
    }

    @Override
    public  AuthToken login(String username, String password) throws RemoteException {
        User user = users.get(username);
        if (user != null && user.password.equals(password)) {
            AuthToken token = new AuthToken(username);
            sessions.put(token.token, token);
            System.out.println("User logged in: " +username);
            return token;
        }
        return null;
    }
    @Override
    public boolean validateToken(AuthToken token) {
        if (token == null) return false;
        AuthToken stored = sessions.get(token.token);
        return stored != null && stored.username.equals(token.username);
    }

    private User getUserByToken(AuthToken token) {
        if (!validateToken(token)) return null;
        return users.get(token.username);
    }
    private FileNode getNode(String uri) throws Exception {
        return (FileNode) Naming.lookup(uri);
    }

    private String getRandomNodeURI() {
        Random rand = new Random();
        return NODE_URIS[rand.nextInt(NODE_URIS.length)];
    }
    @Override
    public boolean createUser(AuthToken token, String username, String password, String department, UserRole role) throws RemoteException {
        User manager = getUserByToken(token);
        if (manager == null || manager.role != UserRole.MANAGER) {
            System.out.println("Create user failed: unauthorized.");
            return false;
        }
        if (users.containsKey(username)) {
            System.out.println("User already exists: " + username);
            return false;
        }
        if (role != UserRole.MANAGER && (department == null || department.isEmpty())) {
            System.out.println("Employee must have a department.");
            return false;
        }
        User newUser = new User(username, password, department, role);
        users.put(username, newUser);
        saveUserToFile(newUser);
        System.out.println("User created: " + username);
        return true;
    }

    private void syncFileToAllNodes(String filepath, byte[] data) {
        for (String nodeUri : NODE_URIS) {
            if (!nodeUri.equals(fileLocations.get(filepath)) && isNodeAlive(nodeUri)) {
                try {
                    FileNode node = getNode(nodeUri);
                    node.writeFile(filepath, data);
                    System.out.println("Synced file to: " + nodeUri);
                } catch (Exception e) {
                    System.out.println("Sync failed to " + nodeUri + ": " + e.getMessage());
                }
            }
        }
    }

    @Override
    public boolean writeFile(AuthToken token, String fileName, byte[] data) throws RemoteException {
        User user = getUserByToken(token);
        if (user == null) {
            System.out.println("Write failed: invalid token.");
            return false;
        }

        String filepath = user.department + "/" + fileName;
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(filepath, k -> new ReentrantReadWriteLock());

        try {
            lock.writeLock().lock();
            if (fileLocations.containsKey(filepath)) {
                System.out.println("File already exists in a node: " + filepath);
                return false;
            }

            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                String nodeUri = getRandomNodeURI();

                try {
                    FileNode node = getNode(nodeUri);
                    node.writeFile(filepath, data);
                    fileLocations.put(filepath, nodeUri);
                    syncFileToAllNodes(filepath, data);
                    System.out.println("File written to: " + nodeUri + "/" + filepath);
                    return true;
                } catch (Exception e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        System.out.println("Write failed after " + MAX_RETRIES + " attempts: " + e.getMessage());
                        return false;
                    }
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteFile(AuthToken token, String fileName) throws RemoteException {
        User user = getUserByToken(token);
        if (user == null) {
            System.out.println("Delete failed: invalid token.");
            return false;
        }
        if (user.role != UserRole.MANAGER && (user.department == null || user.department.isEmpty())) {
            System.out.println("Unauthorized: user has no department assigned.");
            return false;
        }

        String filepath = user.department + "/" + fileName;
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(filepath, k -> new ReentrantReadWriteLock());

        try {
            lock.writeLock().lock();
            String nodeUri = fileLocations.get(filepath);
            if (nodeUri == null) {
                System.out.println("File not found in any node.");
                return false;
            }
            try {
                FileNode node = getNode(nodeUri);
                node.deleteFile(filepath);
                fileLocations.remove(filepath);
                fileLocks.remove(filepath);
                return true;
            } catch (Exception e) {
                System.out.println("Delete failed: " + e.getMessage());
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public byte[] readFile(AuthToken token, String fileName) throws RemoteException {
        User user = getUserByToken(token);
        if (user == null) {
            System.out.println("Read failed: invalid token.");
            return null;
        }

        String filepath = user.department + "/" + fileName;
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(filepath, k -> new ReentrantReadWriteLock());

        try {
            lock.readLock().lock();
            int attempts = 0;
            while (attempts < MAX_RETRIES) {
                try {
                    String nodeUri = findNodeWithFile(filepath);
                    FileNode node = getNode(nodeUri);
                    return node.readFile(filepath);
                } catch (Exception e) {
                    attempts++;
                    if (attempts >= MAX_RETRIES) {
                        System.out.println("Read failed after " + MAX_RETRIES + " attempts: " + e.getMessage());
                        return null;
                    }
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public boolean logout(AuthToken token) throws RemoteException {
        if (token == null || !sessions.containsKey(token.token)) {
            System.out.println("Logout failed: invalid token.");
            return false;
        }
        sessions.remove(token.token);
        System.out.println("User logged out: " + token.username);
        return true;
    }

    @Override
    public byte[] readAnyFile(AuthToken token, String department, String fileName) throws RemoteException {
        User user = getUserByToken(token);
        if (user == null) {
            System.out.println("External Read failed: invalid token.");
            return null;
        }

        String filepath = department + "/" + fileName;
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(filepath, k -> new ReentrantReadWriteLock());

        try {
            lock.readLock().lock();

            String nodeUri = loadBalancer.getNextNode();
            int attempts = 0;

            while (attempts < NODE_URIS.length) {
                try {
                    FileNode node = getNode(nodeUri);
                    if (node.fileExists(filepath)) {
                        System.out.println("External read from node: " + nodeUri);
                        return node.readFile(filepath);
                    }
                } catch (Exception e) {
                    System.out.println("Search failed on node " + nodeUri + ": " + e.getMessage());
                }

                nodeUri = loadBalancer.getNextNode();
                attempts++;
            }

            System.out.println("File not found in any node.");
            return null;

        } finally {
            lock.readLock().unlock();
        }
    }
    @Override
    public boolean updateFile(AuthToken token, String fileName, byte[] data) throws RemoteException {
        User user = getUserByToken(token);
        if (user == null) {
            System.out.println("Update failed: invalid token.");
            return false;
        }

        String filepath = user.department + "/" + fileName;
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(filepath, k -> new ReentrantReadWriteLock());

        try {
            lock.writeLock().lock();
            String nodeUri = fileLocations.get(filepath);
            if (nodeUri == null) {
                System.out.println("File not found for update.");
                return false;
            }

            try {
                FileNode node = getNode(nodeUri);
                node.updateFile(filepath, data);
                syncFileToAllNodes(filepath, data);
                return true;
            } catch (Exception e) {
                System.out.println("Update failed: " + e.getMessage());
                return false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }



}
