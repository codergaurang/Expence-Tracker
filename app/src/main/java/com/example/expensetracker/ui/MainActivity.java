// app/src/main/java/com/example/expensetracker/ui/MainActivity.java
package com.example.expensetracker.ui;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.adapter.TransactionAdapter;
import com.example.expensetracker.db.DbHelper;
import com.example.expensetracker.sms.SmsImporter;
import com.example.expensetracker.sms.SmsReceiver;
import com.example.expensetracker.util.CsvExporter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements DescriptionDialogFragment.Callback {

    private static final int REQ_SMS = 2001;
    private static final int REQ_NOTIF = 2002;

    private DbHelper db;
    private TransactionAdapter adapter;

    private FrameLayout loadingOverlay;
    private ProgressBar progressBar;
    private TextView tvLoading;

    private final ExecutorService io = Executors.newSingleThreadExecutor();

    private final BroadcastReceiver newTxReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            reload();
            showNextMandatoryDescriptionIfAny();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DbHelper(this);

        RecyclerView rv = findViewById(R.id.rvTransactions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(t -> {
            Intent i = new Intent(MainActivity.this, TransactionDetailActivity.class);
            i.putExtra("tx_id", t.rowId);
            startActivity(i);
        });
        rv.setAdapter(adapter);

        loadingOverlay = findViewById(R.id.loadingOverlay);
        progressBar = findViewById(R.id.progressBar);
        tvLoading = findViewById(R.id.tvLoading);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AddCashActivity.class)));

        Button btnExport = findViewById(R.id.btnExport);
        btnExport.setOnClickListener(v -> openDateRangeAndExport());

        ensureSmsPermissions();
        ensureNotificationPermission();

        handleOpenReasonIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleOpenReasonIntent(intent);
    }

    private void handleOpenReasonIntent(Intent intent) {
        if (intent == null) return;
        if (!"OPEN_REASON".equals(intent.getAction())) return;

        long txId = intent.getLongExtra("tx_id", -1);
        if (txId != -1) {
            // show dialog for specific tx
            showMandatoryDescriptionFor(txId);
        }
    }

    private void showMandatoryDescriptionFor(long txId) {
        if (getSupportFragmentManager().findFragmentByTag("desc") != null) return;
        DescriptionDialogFragment.newInstance(txId)
                .show(getSupportFragmentManager(), "desc");
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(newTxReceiver, new IntentFilter(SmsReceiver.ACTION_NEW_TX));
        reload();
        showNextMandatoryDescriptionIfAny();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(newTxReceiver);
    }

    private void ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIF);
            }
        }
    }

    private void ensureSmsPermissions() {
        boolean readGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED;
        boolean recvGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;

        if (!readGranted || !recvGranted) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS},
                    REQ_SMS);
        } else {
            maybeFirstLaunchImport();
        }
    }

    private void maybeFirstLaunchImport() {
        SharedPreferences sp = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean imported = sp.getBoolean("imported_sms", false);
        if (!imported) {
            importSmsInBackground();
        } else {
            if (!sp.getBoolean("desc_for_new_only", false)) {
                sp.edit().putBoolean("desc_for_new_only", true).apply();
            }
        }
    }

    private void importSmsInBackground() {
        setLoading(true, "Importing previous UPI transactions from SMS...");
        io.execute(() -> {
            int count = SmsImporter.importAllUpiFromInbox(MainActivity.this, db);

            runOnUiThread(() -> {
                setLoading(false, "");
                getSharedPreferences("prefs", MODE_PRIVATE).edit()
                        .putBoolean("imported_sms", true)
                        .putBoolean("desc_for_new_only", true)
                        .apply();

                reload();
                Toast.makeText(this, "Imported " + count + " old UPI transactions", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void reload() {
        List<com.example.expensetracker.model.Transaction> list = db.getAllTransactionsDesc();
        adapter.setItems(list);
    }

    private void setLoading(boolean show, String msg) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(true);
        tvLoading.setText(msg == null ? "" : msg);
    }

    private void showNextMandatoryDescriptionIfAny() {
        SharedPreferences sp = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean newOnly = sp.getBoolean("desc_for_new_only", false);
        if (!newOnly) return;

        long pendingId = db.getNextPendingDescriptionTxId(); // rt_ only
        if (pendingId == -1) return;

        showMandatoryDescriptionFor(pendingId);
    }

    @Override
    public void onDescriptionSaved() {
        reload();
        showNextMandatoryDescriptionIfAny();
    }

    private void openDateRangeAndExport() {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog startPicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar start = Calendar.getInstance();
                    start.set(year, month, dayOfMonth, 0, 0, 0);
                    start.set(Calendar.MILLISECOND, 0);

                    DatePickerDialog endPicker = new DatePickerDialog(this,
                            (view2, year2, month2, day2) -> {
                                Calendar end = Calendar.getInstance();
                                end.set(year2, month2, day2, 23, 59, 59);
                                end.set(Calendar.MILLISECOND, 999);

                                exportRange(start.getTimeInMillis(), end.getTimeInMillis());
                            },
                            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
                    endPicker.setTitle("Select end date");
                    endPicker.show();
                },
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

        startPicker.setTitle("Select start date");
        startPicker.show();
    }

    private void exportRange(long startMillis, long endMillis) {
        setLoading(true, "Exporting CSV...");
        io.execute(() -> {
            try {
                List<com.example.expensetracker.model.Transaction> txs = db.getBetween(startMillis, endMillis);
                String fileName = "ExpenseTracker_" + startMillis + "_" + endMillis + ".csv";
                Uri uri = CsvExporter.exportToDownloadsCsv(MainActivity.this, fileName, txs);

                runOnUiThread(() -> {
                    setLoading(false, "");
                    Toast.makeText(this, "Exported: " + uri, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false, "");
                    Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_SMS) {
            boolean ok = true;
            for (int r : grantResults) ok = ok && (r == PackageManager.PERMISSION_GRANTED);
            if (ok) {
                maybeFirstLaunchImport();
            } else {
                Toast.makeText(this, "SMS permission is required to auto-detect UPI transactions.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
