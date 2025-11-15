package service;

import main.*;
import util.Constants;

import java.io.*;

/**
 * AuthService: load/save users and manage users_current.txt
 */
public class AuthService {
    private String usersAllFilePath;
    private String usersCurrentFilePath;
    private User[] registeredUsers;
    private int registeredCount;

    public AuthService(String usersAllFilePath, String usersCurrentFilePath) {
        this.usersAllFilePath = usersAllFilePath;
        this.usersCurrentFilePath = usersCurrentFilePath;
        this.registeredUsers = new User[500];
        this.registeredCount = 0;
        loadUsersFromFile();
    }

    public String getUsersAllFilePath() { return usersAllFilePath; }
    public String getUsersCurrentFilePath() { return usersCurrentFilePath; }

    public void loadUsersFromFile() {
        File f = new File(usersAllFilePath);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length < 5) continue;
                String username = parts[0];
                String hash = parts[1];
                String name = parts[2];
                String email = parts[3];
                String role = parts[4];
                User u = null;
                if (role.equals(Constants.ROLE_CUSTOMER)) u = new Customer(username, hash, name, email);
                else if (role.equals(Constants.ROLE_ADMIN)) u = new Admin(username, hash, name, email, "adminkey");
                else if (role.equals(Constants.ROLE_ATTENDANT)) u = new Attendant(username, hash, name, email, "SHIFT");
                if (u != null && registeredCount < registeredUsers.length) registeredUsers[registeredCount++] = u;
            }
        } catch (IOException e) {
            System.out.println("Error loading users: " + e.getMessage());
        }
    }

    public void saveUserToFile(User u) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(usersAllFilePath, true))) {
            // Note: hashedPassword and email are protected in User; using getters where available
            String hashed = u.getHashedPassword();
            String email = u.getEmail();
            String line = String.join("|", new String[]{u.getUsername(), hashed, u.getName(), email, u.getRole()});
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error saving user: " + e.getMessage());
        }
    }

    public void registerUser(User u) {
        if (registeredCount >= registeredUsers.length) return;
        registeredUsers[registeredCount++] = u;
        saveUserToFile(u);
    }

    public User login(String username, String password) {
        User u = findUserByUsername(username);
        if (u == null) return null;
        if (!u.checkPassword(password)) return null;
        String token = generateSessionToken(username);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(usersCurrentFilePath, true))) {
            String line = String.join("|", new String[]{username, token, "", "", "0"});
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error writing current users: " + e.getMessage());
        }
        return u;
    }

    public void logoutByUsername(String username) {
        File f = new File(usersCurrentFilePath);
        if (!f.exists()) return;
        File tmp = new File(usersCurrentFilePath + ".tmp");
        try (BufferedReader br = new BufferedReader(new FileReader(f));
             BufferedWriter bw = new BufferedWriter(new FileWriter(tmp))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                if (parts.length >= 1 && parts[0].equals(username)) continue; // skip
                bw.write(line);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error updating current users: " + e.getMessage());
        }
        // replace original file with temp
        if (tmp.exists()) {
            f.delete();
            tmp.renameTo(f);
        }
    }

    public User findUserByUsername(String username) {
        for (int i = 0; i < registeredCount; i++) {
            if (registeredUsers[i] != null && registeredUsers[i].getUsername().equals(username)) return registeredUsers[i];
        }
        return null;
    }

    public String generateSessionToken(String username) {
        return username + "_" + System.currentTimeMillis();
    }
}
