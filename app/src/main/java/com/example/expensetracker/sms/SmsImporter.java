// app/src/main/java/com/example/expensetracker/sms/SmsImporter.java
package com.example.expensetracker.sms;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import com.example.expensetracker.db.DbHelper;
import com.example.expensetracker.model.Transaction;

public class SmsImporter {

    public static int importAllUpiFromInbox(Context ctx, DbHelper db) {
        int imported = 0;

        Uri inbox = Telephony.Sms.Inbox.CONTENT_URI;

        String[] projection = new String[] {
                Telephony.Sms._ID,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
        };

        Cursor c = ctx.getContentResolver().query(
                inbox,
                projection,
                null,
                null,
                Telephony.Sms.DEFAULT_SORT_ORDER
        );

        if (c == null) return 0;

        while (c.moveToNext()) {
            String id = c.getString(0);
            String body = c.getString(1);
            long dateMillis = c.getLong(2);

            String smsUniqueId = "inbox_" + id;

            if (db.existsBySmsUniqueId(smsUniqueId)) continue;

            Transaction t = SmsParser.parseUpiTransaction(body, smsUniqueId);
            if (t == null) continue;

            // If SMS had no "on dd-MM-yyyy ..." timestamp, fallback to provider date
            if (t.txTimeMillis <= 0) t.txTimeMillis = dateMillis;

            long row = db.insertTransaction(t);
            if (row != -1) imported++;
        }

        c.close();
        return imported;
    }
}
