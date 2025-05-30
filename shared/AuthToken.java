package shared;

import java.io.Serializable;
import java.util.UUID;

public class AuthToken implements Serializable {
    public String token;
    public String username;
    public String department;
    public long createdAt;

    public AuthToken(String username) {
        this.username = username;
        this.token = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }
}
