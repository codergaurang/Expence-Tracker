// app/src/main/java/com/example/expensetracker/db/DbHelper.java
package com.example.expensetracker.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.expensetracker.model.Transaction;

import java.util.ArrayList;
import java.util.List;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "expense_tracker.db";
    public static final int DB_VERSION = 1;

    public static final String T_TX = "transactions";

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + T_TX + " (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "amount REAL NOT NULL," +
                        "tx_time_millis INTEGER NOT NULL," +
                        "direction TEXT NOT NULL," +
                        "mode TEXT NOT NULL," +
                        "txn_id TEXT," +
                        "upi_id TEXT," +
                        "description TEXT," +
                        "sms_unique_id TEXT UNIQUE," +
                        "raw_body TEXT" +
                        ")"
        );
        db.execSQL("CREATE INDEX idx_tx_time ON " + T_TX + "(tx_time_millis)");
        db.execSQL("CREATE INDEX idx_desc ON " + T_TX + "(description)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + T_TX);
        onCreate(db);
    }

    public long insertTransaction(Transaction t) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("amount", t.amount);
        cv.put("tx_time_millis", t.txTimeMillis);
        cv.put("direction", t.direction);
        cv.put("mode", t.mode);
        cv.put("txn_id", t.txnId);
        cv.put("upi_id", t.upiId);
        cv.put("description", t.description);
        cv.put("sms_unique_id", t.smsUniqueId);
        cv.put("raw_body", t.rawBody);
        return db.insertWithOnConflict(T_TX, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean existsBySmsUniqueId(String smsUniqueId) {
        if (smsUniqueId == null) return false;
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + T_TX + " WHERE sms_unique_id = ? LIMIT 1",
                new String[]{smsUniqueId});
        boolean exists = c.moveToFirst();
        c.close();
        return exists;
    }

    public List<Transaction> getAllTransactionsDesc() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, amount, tx_time_millis, direction, mode, txn_id, upi_id, description, sms_unique_id, raw_body " +
                        "FROM " + T_TX + " ORDER BY tx_time_millis DESC, id DESC",
                null
        );
        List<Transaction> list = new ArrayList<>();
        while (c.moveToNext()) {
            list.add(fromCursor(c));
        }
        c.close();
        return list;
    }

    public Transaction getById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, amount, tx_time_millis, direction, mode, txn_id, upi_id, description, sms_unique_id, raw_body " +
                        "FROM " + T_TX + " WHERE id = ? LIMIT 1",
                new String[]{String.valueOf(id)}
        );
        Transaction t = null;
        if (c.moveToFirst()) t = fromCursor(c);
        c.close();
        return t;
    }

    /**
     * Returns the next transaction that must be described, but ONLY for transactions
     * detected after installation (SmsReceiver creates sms_unique_id starting with "rt_").
     *
     * Imported old SMS use sms_unique_id like "inbox_<id>" and are excluded.
     */
    public long getNextPendingDescriptionTxId() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id FROM " + T_TX +
                        " WHERE (description IS NULL OR TRIM(description) = '')" +
                        " AND sms_unique_id LIKE 'rt_%'" +
                        " ORDER BY tx_time_millis ASC, id ASC LIMIT 1",
                null
        );
        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public int updateDescription(long id, String description) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("description", description);
        return db.update(T_TX, cv, "id = ?", new String[]{String.valueOf(id)});
    }

    public List<Transaction> getBetween(long startMillisInclusive, long endMillisInclusive) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT id, amount, tx_time_millis, direction, mode, txn_id, upi_id, description, sms_unique_id, raw_body " +
                        "FROM " + T_TX +
                        " WHERE tx_time_millis >= ? AND tx_time_millis <= ?" +
                        " ORDER BY tx_time_millis ASC, id ASC",
                new String[]{String.valueOf(startMillisInclusive), String.valueOf(endMillisInclusive)}
        );
        List<Transaction> list = new ArrayList<>();
        while (c.moveToNext()) list.add(fromCursor(c));
        c.close();
        return list;
    }

    private Transaction fromCursor(Cursor c) {
        Transaction t = new Transaction();
        t.rowId = c.getLong(0);
        t.amount = c.getDouble(1);
        t.txTimeMillis = c.getLong(2);
        t.direction = c.getString(3);
        t.mode = c.getString(4);
        t.txnId = c.getString(5);
        t.upiId = c.getString(6);
        t.description = c.getString(7);
        t.smsUniqueId = c.getString(8);
        t.rawBody = c.getString(9);
        return t;
    }
}
