// app/src/main/java/com/example/expensetracker/sms/SmsParser.java
package com.example.expensetracker.sms;

import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.util.TimeFormat;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {

    // Strong gate: start like bank statement
    private static final Pattern STARTS_LIKE_BANK =
            Pattern.compile("^\\s*Your\\s+a/c\\s+no\\.", Pattern.CASE_INSENSITIVE);

    // Strong gate: must contain explicit timestamp like "on 28-12-2025 12:06:35"
    private static final Pattern DT_ON =
            Pattern.compile("\\bon\\s+(\\d{2}-\\d{2}-\\d{4}\\s+\\d{2}:\\d{2}:\\d{2})\\b",
                    Pattern.CASE_INSENSITIVE);

    // Amount: Rs.200.00 / Rs 200 / Rs. 2,000.50
    private static final Pattern AMOUNT =
            Pattern.compile("\\bRs\\.?\\s*([0-9,]+(?:\\.\\d{1,2})?)\\b", Pattern.CASE_INSENSITIVE);

    // VPA: first thing that looks like name@bank
    private static final Pattern VPA =
            Pattern.compile("\\b([a-z0-9._\\-]{2,}@[a-z0-9._\\-]{2,})\\b", Pattern.CASE_INSENSITIVE);

    // Reference: supports:
    // (RefNo 123) , Ref No: 123 , UPI Ref no 123 , UPI Ref No. 123
    private static final Pattern REF =
            Pattern.compile("\\b(?:Ref\\s*No|RefNo|UPI\\s*Ref\\s*No)\\s*\\.?\\s*[:\\-]?\\s*(\\d{6,})\\b",
                    Pattern.CASE_INSENSITIVE);

    public static Transaction parseUpiTransaction(String smsBody, String smsUniqueId) {
        if (smsBody == null) return null;
        String body = smsBody.trim();
        if (body.isEmpty()) return null;

        // Gate 1: must start like bank a/c alert
        if (!STARTS_LIKE_BANK.matcher(body).find()) return null;

        // Gate 2: must have "on dd-MM-yyyy HH:mm:ss"
        Matcher mDt = DT_ON.matcher(body);
        if (!mDt.find()) return null;
        long timeMillis = TimeFormat.parseSmsDateTimeToMillis(mDt.group(1));

        // Decide direction
        String lower = body.toLowerCase(Locale.getDefault());
        boolean hasDebited = lower.contains("debited");
        boolean hasCredited = lower.contains("credited");

        // Must have at least one
        if (!hasDebited && !hasCredited) return null;

        // Amount required
        Double amount = extractAmount(body);
        if (amount == null || amount <= 0) return null;

        // Must look like UPI: require either VPA OR "upi" keyword
        boolean looksUpi = lower.contains(" upi") || lower.contains("upi ") || VPA.matcher(body).find();
        if (!looksUpi) return null;

        String direction;
        // If both words exist, decide using typical patterns:
        // - "credited ... and debited from VPA ..." should be RECEIVED
        if (hasCredited && (lower.contains("debited from vpa") || lower.contains("credited"))) {
            direction = "RECEIVED";
        } else if (hasDebited && !hasCredited) {
            direction = "PAID";
        } else if (hasCredited && !hasDebited) {
            direction = "RECEIVED";
        } else {
            // fallback: treat as PAID if it contains "trf to" / "paid to"
            if (lower.contains("trf to") || lower.contains("paid to") || lower.contains("to ")) direction = "PAID";
            else direction = "RECEIVED";
        }

        String upiId = extractUpiId(body);
        String ref = extractRef(body);

        // At least one of UPI id / ref should exist; otherwise ignore to avoid random matches
        if ((upiId == null || upiId.isEmpty()) && (ref == null || ref.isEmpty())) return null;

        Transaction t = new Transaction();
        t.amount = amount;
        t.txTimeMillis = timeMillis;
        t.direction = direction;
        t.mode = "UPI";
        t.upiId = (upiId == null) ? "" : upiId;
        t.txnId = (ref == null) ? "" : ref;
        t.description = ""; // will be enforced only for new rt_ transactions in your DB logic
        t.smsUniqueId = smsUniqueId;
        t.rawBody = body;
        return t;
    }

    private static Double extractAmount(String body) {
        Matcher m = AMOUNT.matcher(body);
        if (!m.find()) return null;
        try {
            return Double.parseDouble(m.group(1).replace(",", "").trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractUpiId(String body) {
        Matcher m = VPA.matcher(body);
        if (m.find()) return m.group(1).trim().toLowerCase(Locale.getDefault());
        return "";
    }

    private static String extractRef(String body) {
        Matcher m = REF.matcher(body);
        if (m.find()) return m.group(1).trim();
        return "";
    }
}
