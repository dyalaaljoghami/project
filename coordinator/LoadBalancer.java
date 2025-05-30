package coordinator;

public interface LoadBalancer {
    String getNextNode();
    String getNextAvailableNode();

}
