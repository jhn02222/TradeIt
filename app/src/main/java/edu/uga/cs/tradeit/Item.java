package edu.uga.cs.tradeit;

public class Item {
    private String itemId;
    private String name;
    private String description;
    private String categoryId;
    private String categoryName;
    private double price;
    private boolean isFree;
    private String postedBy;
    private String postedByName;
    private long postedAt;
    private String status; // "available", "pending", "sold"

    public Item() {} // Required for Firebase

    public Item(String itemId, String name, String description, String categoryId,
                String categoryName, double price, boolean isFree, String postedBy,
                String postedByName, long postedAt, String status) {
        this.itemId = itemId;
        this.name = name;
        this.description = description;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.price = price;
        this.isFree = isFree;
        this.postedBy = postedBy;
        this.postedByName = postedByName;
        this.postedAt = postedAt;
        this.status = status;
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategoryId() { return categoryId; }
    public void setCategoryId(String categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public boolean isFree() { return isFree; }
    public void setFree(boolean free) { isFree = free; }
    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
    public String getPostedByName() { return postedByName; }
    public void setPostedByName(String postedByName) { this.postedByName = postedByName; }
    public long getPostedAt() { return postedAt; }
    public void setPostedAt(long postedAt) { this.postedAt = postedAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

