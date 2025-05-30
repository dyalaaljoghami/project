package client;

import coordinator.CoordinatorService;
import shared.AuthToken;
import shared.UserRole;

import java.rmi.Naming;
import java.util.Scanner;

public class ClientApp {
    public static void main(String[] args) {
        try {
            CoordinatorService coordinator = (CoordinatorService)
                    Naming.lookup("rmi://localhost:2000/CoordinatorService");

            Scanner sc = new Scanner(System.in);

            System.out.print("Username: ");
            String username = sc.nextLine();
            System.out.print("Password: ");
            String password = sc.nextLine();

            AuthToken token = coordinator.login(username, password);
            if (token == null) {
                System.out.println("Login failed.");
                return;
            }

            UserRole role = coordinator.getUserRole(token);
            if (role == null) {
                System.out.println("Cannot determine user role.");
                return;
            }

            System.out.println("Logged in as: " + role);
            if (role == UserRole.MANAGER) {
                System.out.println("You can create employee accounts.");
                // مدير يمكنه إنشاء مستخدمين
                while (true) {
                    System.out.println("\nChoose an option:");
                    System.out.println("1. Create  employee");
                    System.out.println("2. Logout");
                    System.out.print("Choice: ");
                    int choice = Integer.parseInt(sc.nextLine());
                    if (choice == 2) {
                        coordinator.logout(token);
                        System.out.println("Logged out.");
                        break;
                    }

                    System.out.print("Employee Username: ");
                    String empUsername = sc.nextLine();
                    System.out.print("Employee Password: ");
                    String empPassword = sc.nextLine();
                    System.out.print("Employee Department (QA, Design, Development): ");
                    String department = sc.nextLine();
                    System.out.print("Employee Role (EMPLOYEE): ");
                    UserRole empRole = UserRole.EMPLOYEE;

                    boolean created = coordinator.createUser(token, empUsername, empPassword, department, empRole);
                    if (created) {
                        System.out.println("Employee created successfully.");
                    } else {
                        System.out.println("Failed to create employee.");
                    }                }
            } else {
                System.out.println("You are an employee.");
                while (true) {
                    System.out.println("\nSelect an operation:");
                    System.out.println("1. Add New File");
                    System.out.println("2. Edit File");
                    System.out.println("3. Delete File");
                    System.out.println("4. Read File");
                    System.out.println("5. Read  from Another Department");
                    System.out.println("6. Logout");
                    System.out.print("Choice: ");
                    int choice = Integer.parseInt(sc.nextLine());

                    if (choice == 6){
                        coordinator.logout(token);
                    System.out.println("Logged out.");
                    break;
                }
                    System.out.print("Filename: ");
                    String filename = sc.nextLine();

                    if (choice == 1 ) {
                        System.out.print("Enter content: ");
                        String content = sc.nextLine();
                        boolean success = coordinator.writeFile(token, filename, content.getBytes());
                        System.out.println(success ? "File written." : "Write failed.");
                    }else if (choice == 2) {
                        System.out.print("Enter new content: ");
                        String content = sc.nextLine();
                        boolean success = coordinator.updateFile(token, filename, content.getBytes());
                        System.out.println(success ? "File updated." : "Update failed.");
                    }else if (choice == 3) {
                        boolean deleted = coordinator.deleteFile(token, filename);
                        System.out.println(deleted ? "File deleted." : "Delete failed.");
                    } else if (choice == 4) {
                        byte[] data = coordinator.readFile(token,filename);
                        if (data != null) {
                            System.out.println("Content:\n" + new String(data));
                        } else {
                            System.out.println("Read failed.");
                        }
                    }else if (choice == 5) {
                        System.out.print("Enter department (QA, Design, Development): ");
                        String dept = sc.nextLine();
                        System.out.print("Filename: ");
                        String fileName = sc.nextLine();
                        byte[] data = coordinator.readAnyFile(token, dept, fileName);
                        if (data != null) {
                            System.out.println("Content:\n" + new String(data));
                        } else {
                            System.out.println("File not found in any node.");
                        }
                    }

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
