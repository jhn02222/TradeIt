package edu.uga.cs.tradeit;

public class Transaction {
    private String transactionId;
    private String itemId;
    private String itemName;
    private String categoryName;
    private String sellerId;
    private String sellerName;
    private String buyerId;
    private String buyerName;
    private double price;
    private long createdAt;
    private String status; // "pending", "completed"
    private boolean sellerConfirmed;
    private boolean buyerConfirmed;
    private long completedAt;

    public Transaction() {} // Required for Firebase

    public Transaction(String transactionId, String itemId, String itemName,
                       String categoryName, String sellerId, String sellerName,
                       String buyerId, String buyerName, double price, long createdAt,
                       String status, boolean sellerConfirmed, boolean buyerConfirmed,
                       long completedAt) {
        this.transactionId = transactionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.categoryName = categoryName;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.buyerId = buyerId;
        this.buyerName = buyerName;
        this.price = price;
        this.createdAt = createdAt;
        this.status = status;
        this.sellerConfirmed = sellerConfirmed;
        this.buyerConfirmed = buyerConfirmed;
        this.completedAt = completedAt;
    }

    // Getters and Setters
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }
    public String getBuyerName() { return buyerName; }
    public void setBuyerName(String buyerName) { this.buyerName = buyerName; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isSellerConfirmed() { return sellerConfirmed; }
    public void setSellerConfirmed(boolean sellerConfirmed) { this.sellerConfirmed = sellerConfirmed; }
    public boolean isBuyerConfirmed() { return buyerConfirmed; }
    public void setBuyerConfirmed(boolean buyerConfirmed) { this.buyerConfirmed = buyerConfirmed; }
    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
}