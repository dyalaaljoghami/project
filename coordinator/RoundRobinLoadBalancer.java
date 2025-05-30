package coordinator;

import node.FileNode;

import java.rmi.Naming;
public class RoundRobinLoadBalancer implements LoadBalancer{
    private final String[] nodes;
    private int currentIndex = 0;

    public RoundRobinLoadBalancer(String[] nodes) {
        this.nodes = nodes;
    }

    @Override
    public synchronized String getNextNode() {
        String node = nodes[currentIndex];
        currentIndex = (currentIndex + 1) % nodes.length;
        return node;
    }
    @Override
    public synchronized String getNextAvailableNode() {
        int startIndex = currentIndex;
        do {
            String node = nodes[currentIndex];
            try {
                FileNode fileNode = (FileNode) Naming.lookup(node);
                if (fileNode.isAlive()) {
                    currentIndex = (currentIndex + 1) % nodes.length;
                    return node;
                }
            } catch (Exception e) {
                System.out.println("Node " + node + " is not available: " + e.getMessage());
            }
            currentIndex = (currentIndex + 1) % nodes.length;
        } while (currentIndex != startIndex);

        throw new RuntimeException("No available nodes");
    }
}
