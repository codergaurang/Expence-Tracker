// app/src/main/java/com/example/expensetracker/model/Transaction.java
package com.example.expensetracker.model;

public class Transaction {
    public long rowId;                // DB primary key
    public double amount;
    public long txTimeMillis;         // epoch millis
    public String direction;          // "PAID" or "RECEIVED"
    public String mode;               // "UPI" or "CASH"
    public String txnId;              // RefNo / UPI Ref no
    public String upiId;              // VPA
    public String description;        // mandatory for UPI detections
    public String smsUniqueId;        // used to avoid duplicates
    public String rawBody;            // optional

    public Transaction() {}
}
