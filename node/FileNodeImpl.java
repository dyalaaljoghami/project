package node;

import java.io.*;
import java.nio.file.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FileNodeImpl extends UnicastRemoteObject implements FileNode {
    private String basePath = "./files";
    private final Map<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();

    public FileNodeImpl() throws RemoteException {
        super();
    }

    private Path resolvePath(String fileName) {
        //String[] parts = fileName.split("/", 2);
        return Paths.get(basePath, fileName);
    }
    @Override
    public List<String> getAllFiles() throws RemoteException {
        try {
            return Files.walk(Paths.get(basePath))
                    .filter(Files::isRegularFile)
                    .map(path -> Paths.get(basePath).relativize(path).toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RemoteException("Failed to list files", e);
        }
    }
    public byte[] readFile(String fileName) throws RemoteException {
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
        try {
            lock.readLock().lock();
            Path path = resolvePath(fileName);

            try (InputStream in = Files.newInputStream(path);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                return out.toByteArray();
            }
        } catch (IOException e) {
            throw new RemoteException("Read failed", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean writeFile(String fileName, byte[] data) throws RemoteException {
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
        try {
            lock.writeLock().lock();
            Path path = resolvePath(fileName);
            Files.createDirectories(path.getParent());
            Files.write(path, data, StandardOpenOption.CREATE_NEW);
            System.out.println("File written: " + fileName);
            return true;
        } catch (IOException e) {
            System.out.println("Write failed: " + e.getMessage());
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    public void deleteFile(String fileName) throws RemoteException {
        try {
            Files.deleteIfExists(resolvePath(fileName));
            System.out.println("File deleted: " + fileName);
        } catch (IOException e) {
            throw new RemoteException("Delete failed", e);
        }
    }
    @Override
    public void updateFile(String fileName, byte[] data) throws RemoteException {
        ReentrantReadWriteLock lock = fileLocks.computeIfAbsent(fileName, k -> new ReentrantReadWriteLock());
        try {
            lock.writeLock().lock();
            Path path = resolvePath(fileName);
            if (!Files.exists(path)) {
                throw new RemoteException("File does not exist.");
            }
            Files.write(path, data, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("File updated: " + fileName);
        } catch (IOException e) {
            throw new RemoteException("Update failed", e);
        } finally {
            lock.writeLock().unlock();
        }
    }
    @Override
    public boolean fileExists(String fileName) throws RemoteException {
        Path path = resolvePath(fileName);
        return Files.exists(path);
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }


}
