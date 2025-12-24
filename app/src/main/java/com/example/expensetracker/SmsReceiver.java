package com.example.expensetracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            if (intent == null || context == null) return;

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                String format = bundle.getString("format");

                if (pdus != null) {
                    for (Object pdu : pdus) {
                        try {
                            SmsMessage smsMessage;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                            } else {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            }

                            String messageBody = smsMessage.getMessageBody();
                            String sender = smsMessage.getOriginatingAddress();

                            Log.d(TAG, "SMS from: " + sender + " Message: " + messageBody);

                            if (isPaymentSMS(messageBody)) {
                                parseAndSaveTransaction(context, messageBody);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing SMS: " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onReceive: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean isPaymentSMS(String message) {
        if (message == null) return false;
        message = message.toLowerCase();
        return (message.contains("debited") || message.contains("credited") ||
                message.contains("paid") || message.contains("received") ||
                message.contains("upi") || message.contains("transaction") ||
                message.contains("rs") || message.contains("inr"));
    }

    private void parseAndSaveTransaction(Context context, String message) {
        try {
            String type = "PAID";
            if (message.toLowerCase().contains("credited") ||
                    message.toLowerCase().contains("received") ||
                    message.toLowerCase().contains("deposited")) {
                type = "RECEIVED";
            }

            double amount = extractAmount(message);
            if (amount > 0) {
                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

                Transaction transaction = new Transaction();
                transaction.setType(type);
                transaction.setAmount(amount);
                transaction.setDateTime(dateTime);
                transaction.setDescription(extractDescription(message));
                transaction.setPaymentMode("UPI");

                DatabaseHelper db = new DatabaseHelper(context);
                db.addTransaction(transaction);

                Log.d(TAG, "Transaction saved: " + type + " - Rs." + amount);

                Intent updateIntent = new Intent("com.example.expensetracker.UPDATE_UI");
                context.sendBroadcast(updateIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing transaction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private double extractAmount(String message) {
        try {
            Pattern pattern = Pattern.compile("(?:rs\\.?|inr)\\s*([0-9,]+\\.?[0-9]*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(message);

            if (matcher.find()) {
                String amountStr = matcher.group(1).replace(",", "");
                return Double.parseDouble(amountStr);
            }

            pattern = Pattern.compile("([0-9,]+\\.[0-9]{2})");
            matcher = pattern.matcher(message);
            if (matcher.find()) {
                String amountStr = matcher.group(1).replace(",", "");
                return Double.parseDouble(amountStr);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting amount: " + e.getMessage());
        }

        return 0.0;
    }

    private String extractDescription(String message) {
        try {
            if (message.length() > 100) {
                return message.substring(0, 100) + "...";
            }
            return message;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting description: " + e.getMessage());
            return "UPI Transaction";
        }
    }
}
