package main;

public abstract class User {
    protected String username;
    protected String password;
    protected String name;
    protected String role;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public User(String username, String password, String name, String role) {
        this.username = username;
        this.password = password;
        this.name = name;
      
        this.role = role;
    }

    public abstract void showDashboard();

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getName() { return name; }
    public String getPassword() { return password; }
   
}