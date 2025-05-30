package sync;

import coordinator.CoordinatorImpl;
import java.io.*;
import java.nio.file.*;
import java.rmi.Naming;
import java.util.*;
import java.util.stream.*;
import node.FileNode;

public class SyncTask extends TimerTask {
    private static final String[] NODE_URIS = {
            "rmi://localhost:2001/FileNodeService",
            "rmi://localhost:2002/FileNodeService",
            "rmi://localhost:2003/FileNodeService"
    };

    @Override
    public void run() {
        System.out.println("Starting daily sync...");
        try {
            // Get list of all files from all nodes
            Map<String, List<String>> nodeFiles = new HashMap<>();
            for (String nodeUri : NODE_URIS) {
                try {
                    FileNode node = (FileNode) Naming.lookup(nodeUri);
                    List<String> files = node.getAllFiles();
                    nodeFiles.put(nodeUri, files);
                    System.out.println("Found " + files.size() + " files in " + nodeUri);
                } catch (Exception e) {
                    System.out.println("Could not connect to " + nodeUri + ": " + e.getMessage());
                }
            }

            // Sync files between nodes
            for (String sourceUri : nodeFiles.keySet()) {
                for (String targetUri : nodeFiles.keySet()) {
                    if (!sourceUri.equals(targetUri)) {
                        syncFileToAllNodes(sourceUri, targetUri, nodeFiles.get(sourceUri));
                    }
                }
            }
            System.out.println("Daily sync completed successfully.");
        } catch (Exception e) {
            System.out.println("Error during sync: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void syncFileToAllNodes(String sourceUri, String targetUri, List<String> files) {
        System.out.println("Syncing from " + sourceUri + " to " + targetUri);
        try {
            FileNode sourceNode = (FileNode) Naming.lookup(sourceUri);
            FileNode targetNode = (FileNode) Naming.lookup(targetUri);

            for (String filePath : files) {
                try {
                    if (!targetNode.fileExists(filePath)) {
                        byte[] data = sourceNode.readFile(filePath);
                        targetNode.writeFile(filePath, data);
                        System.out.println("Synced: " + filePath);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to sync " + filePath + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Node sync failed: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        new SyncTask().run();
        System.out.println("Manual synchronization completed!");

    }
}