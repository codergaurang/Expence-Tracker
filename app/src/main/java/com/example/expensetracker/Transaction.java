package com.example.expensetracker;

public class Transaction {
    private int id;
    private String type;
    private double amount;
    private String dateTime;
    private String description;
    private String paymentMode;

    public Transaction() {}

    public Transaction(int id, String type, double amount, String dateTime, String description, String paymentMode) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.dateTime = dateTime;
        this.description = description;
        this.paymentMode = paymentMode;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDateTime() { return dateTime; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPaymentMode() { return paymentMode; }
    public void setPaymentMode(String paymentMode) { this.paymentMode = paymentMode; }
}
