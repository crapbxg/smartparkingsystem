package service;

import main.User;
import main.Admin;
import main.Attendant;
import main.Customer;
import util.Constants;
import exceptions.UserAlreadyExistsException;
import java.io.*;
import java.util.Arrays;

public class AuthService {
    private final String recordFilePath;
    private String[] currentUsers;
    private int currentUserCount;

    public AuthService(String recordFilePath, int totalSpotCount) {
        this.recordFilePath = recordFilePath;
        this.currentUsers = new String[totalSpotCount];
        this.currentUserCount = 0;
    }

    public boolean registerUser(User u) throws UserAlreadyExistsException {
        if (u == null) return false;
        if (findUserByUsername(u.getUsername()) != null) {
            throw new UserAlreadyExistsException("Username already exists: " + u.getUsername());
        }

        saveUserToFile(u, true);
        return true;
    }

    public User findUserByUsername(String username) {
        if (username == null) return null;
        File f = new File(recordFilePath);
        if (!f.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
             
                String[] parts = line.split(";"); 
                if (parts.length < 5) continue;

                String fileUsername = parts[0];

                if (fileUsername.equals(username)) {
                    String password = parts[1];
                    String role = parts[2];
                    String name = parts[3];
                    double balance = Double.parseDouble(parts[4]);

                    User u = null;
                    switch (role) {
                        case Constants.ROLE_ADMIN:
                            u = new Admin(username, password, name);
                            break;
                        case Constants.ROLE_ATTENDANT:
                            u = new Attendant(username, password, name);
                            break;
                        case Constants.ROLE_CUSTOMER:
                            u = new Customer(username, password, name, balance);
                         
                            if (parts.length > 5 && !parts[5].isEmpty()) {
                                String[] plates = parts[5].split(",");
                                for (String p : plates) {
                                    if(!p.trim().isEmpty()) {
                                        ((Customer) u).addVehicle(p.trim());
                                    }
                                }
                            }
                            break;
                    }
                    return u;
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.out.println("Error reading record file: " + e.getMessage());
        }
        return null;
    }

    public User login(String username, String password) {
        User u = findUserByUsername(username);
        if (u == null) return null;
        if (!u.checkPassword(password)) return null;
        return u;
    }

    public void addCurrentUser(String username) {
        if (currentUserCount < currentUsers.length) {
            currentUsers[currentUserCount++] = username;
        }
    }

    public void removeCurrentUser(String username) {
        int foundIndex = -1;
        for (int i = 0; i < currentUserCount; i++) {
            if (currentUsers[i] != null && currentUsers[i].equals(username)) {
                foundIndex = i;
                break;
            }
        }
        if (foundIndex != -1) {
            for (int i = foundIndex; i < currentUserCount - 1; i++) {
                currentUsers[i] = currentUsers[i + 1];
            }
            currentUsers[currentUserCount - 1] = null;
            currentUserCount--;
        }
    }

    public String[] readCurrentUsers() {
        return Arrays.copyOf(currentUsers, currentUserCount);
    }

    public boolean updateUser(User u) {
        if (u == null) return false;

        File f = new File(recordFilePath);
        File temp = new File(recordFilePath + ".tmp");
        boolean updated = false;

        try (BufferedReader br = new BufferedReader(new FileReader(f));
             BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {

            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String fileUsername = line.split(";", 2)[0];

                if (fileUsername.equals(u.getUsername())) {
                    bw.write(formatUserLine(u));
                    bw.newLine();
                    updated = true;
                } else {
                    bw.write(line);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.out.println("Error updating record: " + e.getMessage());
            return false;
        }

        if (updated) {
            f.delete();
            temp.renameTo(f);
        } else {
            temp.delete();
        }
        return updated;
    }

    private String formatUserLine(User u) {
        double balance = 0.0;
        String vehicles = "";

        if (u instanceof Customer) {
            Customer c = (Customer) u;
            balance = c.getAccount().getBalance();
            // Join all plates with commas
            vehicles = String.join(",", c.getVehiclePlates());
        }

        return String.join(";",
            u.getUsername(),
            u.getPassword(),
            u.getRole(),
            u.getName(),
            String.valueOf(balance),
            vehicles 
        );
    }

    private void saveUserToFile(User u, boolean append) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(recordFilePath, append))) {
            bw.write(formatUserLine(u));
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error writing to record file: " + e.getMessage());
        }
    }
}