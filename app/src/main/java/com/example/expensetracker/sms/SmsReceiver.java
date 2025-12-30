// app/src/main/java/com/example/expensetracker/sms/SmsReceiver.java
package com.example.expensetracker.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

import com.example.expensetracker.db.DbHelper;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.util.NotificationUtils;

public class SmsReceiver extends BroadcastReceiver {

    public static final String ACTION_NEW_TX = "com.example.expensetracker.ACTION_NEW_TX";

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) return;

        String format = bundle.getString("format");

        StringBuilder fullBody = new StringBuilder();
        long ts = 0;
        String from = "";

        for (Object pdu : pdus) {
            SmsMessage sms = (format != null)
                    ? SmsMessage.createFromPdu((byte[]) pdu, format)
                    : SmsMessage.createFromPdu((byte[]) pdu);

            if (sms == null) continue;
            if (from.isEmpty() && sms.getOriginatingAddress() != null) from = sms.getOriginatingAddress();
            fullBody.append(sms.getMessageBody());
            ts = Math.max(ts, sms.getTimestampMillis());
        }

        String body = fullBody.toString().trim();
        if (body.isEmpty()) return;

        String smsUniqueId = "rt_" + ts + "_" + (from == null ? "" : from) + "_" + Integer.toHexString(body.hashCode());

        DbHelper db = new DbHelper(context);
        if (db.existsBySmsUniqueId(smsUniqueId)) return;

        Transaction t = SmsParser.parseUpiTransaction(body, smsUniqueId);
        if (t == null) return;

        if (t.txTimeMillis <= 0) t.txTimeMillis = (ts > 0 ? ts : System.currentTimeMillis());

        long txRowId = db.insertTransaction(t);
        if (txRowId == -1) return;

        // Notify UI if app is open
        Intent ui = new Intent(ACTION_NEW_TX);
        context.sendBroadcast(ui);

        // If description is still empty -> show notification so user can open app and enter reason
        boolean needsReason = (t.description == null || t.description.trim().isEmpty());
        if (needsReason) {
            NotificationUtils.showNewTxNeedsReason(context, txRowId, t);
        }
    }
}
