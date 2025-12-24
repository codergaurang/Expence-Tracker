package com.example.expensetracker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fabAdd, fabPdf;
    private TextView emptyView;

    private static final int PERMISSION_REQUEST_CODE = 100;

    private final BroadcastReceiver updateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadTransactions();
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            dbHelper = new DatabaseHelper(this);

            recyclerView = findViewById(R.id.recyclerView);
            fabAdd = findViewById(R.id.fabAdd);
            fabPdf = findViewById(R.id.fabPdf);
            emptyView = findViewById(R.id.emptyView);

            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            adapter = new TransactionAdapter(new ArrayList<>());
            recyclerView.setAdapter(adapter);

            checkPermissions();
            loadTransactions();

            fabAdd.setOnClickListener(v -> showAddTransactionDialog());
            fabPdf.setOnClickListener(v -> showPdfDialog());

            IntentFilter filter = new IntentFilter("com.example.expensetracker.UPDATE_UI");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(updateReceiver, filter, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(updateReceiver, filter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void checkPermissions() {
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            permissions = new String[]{
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    private void loadTransactions() {
        try {
            List<Transaction> transactions = dbHelper.getAllTransactions();

            if (transactions.isEmpty()) {
                if (emptyView != null) emptyView.setVisibility(View.VISIBLE);
                if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            } else {
                if (emptyView != null) emptyView.setVisibility(View.GONE);
                if (recyclerView != null) recyclerView.setVisibility(View.VISIBLE);
                if (adapter != null) adapter.updateTransactions(transactions);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAddTransactionDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null);

            EditText etAmount = dialogView.findViewById(R.id.etAmount);
            EditText etDate = dialogView.findViewById(R.id.etDate);
            EditText etTime = dialogView.findViewById(R.id.etTime);
            EditText etDescription = dialogView.findViewById(R.id.etDescription);
            Spinner spinnerType = dialogView.findViewById(R.id.spinnerType);

            Calendar calendar = Calendar.getInstance();

            ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                    new String[]{"PAID", "RECEIVED"});
            typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerType.setAdapter(typeAdapter);

            etDate.setOnClickListener(v -> {
                new DatePickerDialog(MainActivity.this, (view, year, month, day) -> {
                    String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day);
                    etDate.setText(dateStr);
                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });

            etTime.setOnClickListener(v -> {
                new TimePickerDialog(MainActivity.this, (view, hourOfDay, minute) -> {
                    String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etTime.setText(timeStr);
                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
            });

            builder.setView(dialogView)
                    .setTitle("Add Cash Transaction")
                    .setPositiveButton("Save", (dialog, which) -> {
                        String amountStr = etAmount.getText().toString().trim();
                        String dateStr = etDate.getText().toString().trim();
                        String timeStr = etTime.getText().toString().trim();
                        String description = etDescription.getText().toString().trim();
                        String type = spinnerType.getSelectedItem().toString();

                        if (amountStr.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Please enter amount", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (dateStr.isEmpty() || timeStr.isEmpty()) {
                            Toast.makeText(MainActivity.this, "Please select date", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        try {
                            double amount = Double.parseDouble(amountStr);
                            String dateTime = dateStr + " " + timeStr + ":00";

                            Transaction transaction = new Transaction();
                            transaction.setAmount(amount);
                            transaction.setType(type);
                            transaction.setDateTime(dateTime);
                            transaction.setDescription(description.isEmpty() ? "Cash Transaction" : description);
                            transaction.setPaymentMode("CASH");

                            dbHelper.addTransaction(transaction);
                            loadTransactions();
                            Toast.makeText(MainActivity.this, "Transaction added successfully!", Toast.LENGTH_SHORT).show();
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showPdfDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pdf_date, null);

            EditText etStartDate = dialogView.findViewById(R.id.etStartDate);
            EditText etEndDate = dialogView.findViewById(R.id.etEndDate);

            Calendar calendar = Calendar.getInstance();

            etStartDate.setOnClickListener(v -> {
                new DatePickerDialog(MainActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d 00:00:00", year, month + 1, dayOfMonth);
                            etStartDate.setText(date);
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });

            etEndDate.setOnClickListener(v -> {
                new DatePickerDialog(MainActivity.this,
                        (view, year, month, dayOfMonth) -> {
                            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d 23:59:59", year, month + 1, dayOfMonth);
                            etEndDate.setText(date);
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
            });

            builder.setView(dialogView)
                    .setTitle("Export PDF Report")
                    .setPositiveButton("Export", (dialog, which) -> {
                        String startDate = etStartDate.getText().toString().trim();
                        String endDate = etEndDate.getText().toString().trim();

                        if (!startDate.isEmpty() && !endDate.isEmpty()) {
                            generatePdf(startDate, endDate);
                        } else {
                            Toast.makeText(MainActivity.this, "Please select both dates", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void generatePdf(String startDate, String endDate) {
        try {
            List<Transaction> transactions = dbHelper.getTransactionsByDateRange(startDate, endDate);

            if (transactions.isEmpty()) {
                Toast.makeText(this, "No transactions found in this date range", Toast.LENGTH_SHORT).show();
                return;
            }

            PdfDocument pdfDocument = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
            PdfDocument.Page page = pdfDocument.startPage(pageInfo);

            Canvas canvas = page.getCanvas();
            Paint paint = new Paint();
            Paint headerPaint = new Paint();

            // Header styling
            headerPaint.setTextSize(18);
            headerPaint.setFakeBoldText(true);
            canvas.drawText("Expense Tracker Report", 50, 50, headerPaint);

            paint.setTextSize(12);
            canvas.drawText("Period: " + startDate.substring(0, 10) + " to " + endDate.substring(0, 10), 50, 80, paint);

            // Table Header
            paint.setTextSize(11);
            paint.setFakeBoldText(true);
            int y = 120;

            canvas.drawText("Amount (₹)", 50, y, paint);
            canvas.drawText("Date & Time", 150, y, paint);
            canvas.drawText("Type", 300, y, paint);
            canvas.drawText("Mode", 400, y, paint);

            paint.setFakeBoldText(false);
            y += 20;

            double totalPaid = 0, totalReceived = 0;
            int pageNumber = 1;

            for (Transaction transaction : transactions) {
                if (y > 800) {
                    pdfDocument.finishPage(page);
                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pageNumber).create();
                    page = pdfDocument.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50;
                }

                paint.setTextSize(10);
                String dateOnly = transaction.getDateTime().substring(0, 10);
                String timeOnly = transaction.getDateTime().substring(11, 16);

                canvas.drawText(String.format("%.2f", transaction.getAmount()), 50, y, paint);
                canvas.drawText(dateOnly + " " + timeOnly, 150, y, paint);
                canvas.drawText(transaction.getType(), 300, y, paint);
                canvas.drawText(transaction.getPaymentMode(), 400, y, paint);

                y += 18;

                if ("PAID".equals(transaction.getType())) {
                    totalPaid += transaction.getAmount();
                } else {
                    totalReceived += transaction.getAmount();
                }
            }

            // Summary
            y += 20;
            paint.setTextSize(12);
            paint.setFakeBoldText(true);
            canvas.drawText("Summary:", 50, y, paint);
            y += 20;

            paint.setTextSize(11);
            canvas.drawText("Total Paid: ₹" + String.format("%.2f", totalPaid), 50, y, paint);
            y += 18;
            canvas.drawText("Total Received: ₹" + String.format("%.2f", totalReceived), 50, y, paint);
            y += 18;
            canvas.drawText("Net: ₹" + String.format("%.2f", (totalReceived - totalPaid)), 50, y, paint);

            pdfDocument.finishPage(page);

            String fileName = "ExpenseReport_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".pdf";
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            File file = new File(downloadDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                pdfDocument.writeTo(fos);
                Toast.makeText(this, "PDF saved: " + fileName, Toast.LENGTH_LONG).show();
            }

            pdfDocument.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "PDF Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(updateReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "Some permissions were denied.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
