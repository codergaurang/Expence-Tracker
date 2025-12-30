// app/src/main/java/com/example/expensetracker/util/NotificationUtils.java
package com.example.expensetracker.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.expensetracker.R;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.ui.MainActivity;

import java.util.Locale;

public class NotificationUtils {

    public static final String CH_TX = "tx_channel";
    public static final int ID_TX = 7001;

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel ch = new NotificationChannel(
                CH_TX,
                "New transactions",
                NotificationManager.IMPORTANCE_HIGH
        );
        ch.setDescription("Notifications for newly detected UPI transactions");

        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) nm.createNotificationChannel(ch);
    }

    private static boolean hasNotificationPermission(Context ctx) {
        if (Build.VERSION.SDK_INT < 33) return true; // no runtime permission pre-Android 13
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void showNewTxNeedsReason(Context ctx, long txId, Transaction t) {
        // If user denied notifications, just skip silently (transaction is still saved in DB).
        if (!hasNotificationPermission(ctx)) return; // permission can be rejected by user [web:277]

        ensureChannel(ctx);

        String title = "New " + (t == null ? "transaction" : t.direction);
        String amount = (t == null) ? "" : ("₹ " + String.format(Locale.getDefault(), "%.2f", t.amount));
        String text = amount.isEmpty() ? "Tap to add reason" : (amount + " • Tap to add reason");

        Intent i = new Intent(ctx, MainActivity.class);
        i.setAction("OPEN_REASON");
        i.putExtra("tx_id", txId);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pi = PendingIntent.getActivity(
                ctx,
                (int) (txId % Integer.MAX_VALUE),
                i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder b = new NotificationCompat.Builder(ctx, CH_TX)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setAutoCancel(true)
                .setContentIntent(pi);

        try {
            NotificationManagerCompat.from(ctx).notify(ID_TX, b.build());
        } catch (SecurityException ignored) {
        }
    }
}
