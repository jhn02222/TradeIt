package edu.uga.cs.tradeit;

public class User {
    private String userId;
    private String email;
    private String displayName;
    private long registrationDate;

    public User() {} // Required for Firebase

    public User(String userId, String email, String displayName, long registrationDate) {
        this.userId = userId;
        this.email = email;
        this.displayName = displayName;
        this.registrationDate = registrationDate;
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public long getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(long registrationDate) { this.registrationDate = registrationDate; }
}