package shared;

import java.io.Serializable;

public class User implements Serializable {
    public String username;
    public String password;
    public String department;
    public UserRole role;

    public User(String username, String password, String department, UserRole role) {
        this.username = username;
        this.password = password;
        this.department = department;
        this.role = role;
    }
}
