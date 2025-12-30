// app/src/main/java/com/example/expensetracker/util/CsvExporter.java
package com.example.expensetracker.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.example.expensetracker.model.Transaction;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvExporter {

    public static Uri exportToDownloadsCsv(Context ctx, String fileName, List<Transaction> txs) throws Exception {
        ContentResolver resolver = ctx.getContentResolver();

        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/ExpenseTracker");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
        }

        Uri collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        Uri item = resolver.insert(collection, values);
        if (item == null) throw new IllegalStateException("Failed to create export file");

        try (OutputStream os = resolver.openOutputStream(item)) {
            if (os == null) throw new IllegalStateException("Failed to open output stream");

            String header = "Amount,DateTime,PaidOrReceived,Mode,TransactionId,UpiId,Description\n";
            os.write(header.getBytes(StandardCharsets.UTF_8));

            for (Transaction t : txs) {
                String line =
                        safe(String.valueOf(t.amount)) + "," +
                                safe(TimeFormat.formatForCsv(t.txTimeMillis)) + "," +
                                safe(t.direction) + "," +
                                safe(t.mode) + "," +
                                safe(t.txnId) + "," +
                                safe(t.upiId) + "," +
                                safeCsv(t.description) + "\n";
                os.write(line.getBytes(StandardCharsets.UTF_8));
            }
            os.flush();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues done = new ContentValues();
            done.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(item, done, null, null);
        }

        return item;
    }

    private static String safe(String s) {
        if (s == null) return "";
        // Avoid commas breaking columns (simple approach: quote everything with commas)
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private static String safeCsv(String s) {
        return safe(s == null ? "" : s.trim());
    }
}
