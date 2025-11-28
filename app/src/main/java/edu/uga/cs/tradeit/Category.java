package edu.uga.cs.tradeit;

public class Category {
    private String categoryId;
    private String name;
    private String createdBy;
    private long createdAt;

    public Category() {}

    public Category(String categoryId, String name, String createdBy, long createdAt) {
        this.categoryId = categoryId;
        this.name = name;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}