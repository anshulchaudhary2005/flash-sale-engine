package com.systemdesign.flashsale.controller;

public class ReservationRequest {
    private String userId;
    private String itemId;
    private int quantity;

    // Getters and Setters (Required for Spring to parse JSON)
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}