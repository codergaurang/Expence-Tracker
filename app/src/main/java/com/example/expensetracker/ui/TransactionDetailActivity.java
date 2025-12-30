// app/src/main/java/com/example/expensetracker/ui/TransactionDetailActivity.java
package com.example.expensetracker.ui;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.R;
import com.example.expensetracker.db.DbHelper;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.util.TimeFormat;

public class TransactionDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        long id = getIntent().getLongExtra("tx_id", -1);

        DbHelper db = new DbHelper(this);
        Transaction t = db.getById(id);

        TextView tvAmount = findViewById(R.id.dAmount);
        TextView tvTime = findViewById(R.id.dDateTime);
        TextView tvDir = findViewById(R.id.dDir);
        TextView tvMode = findViewById(R.id.dMode);
        TextView tvTxn = findViewById(R.id.dTxnId);
        TextView tvUpi = findViewById(R.id.dUpiId);
        TextView tvDesc = findViewById(R.id.dDesc);

        if (t == null) {
            tvAmount.setText("-");
            tvTime.setText("-");
            tvDir.setText("-");
            tvMode.setText("-");
            tvTxn.setText("-");
            tvUpi.setText("-");
            tvDesc.setText("-");
            return;
        }

        tvAmount.setText("₹ " + String.format("%.2f", t.amount));
        tvTime.setText(TimeFormat.formatForUi(t.txTimeMillis));
        tvDir.setText(t.direction);
        tvMode.setText(t.mode);
        tvTxn.setText((t.txnId == null || t.txnId.trim().isEmpty()) ? "-" : t.txnId);
        tvUpi.setText((t.upiId == null || t.upiId.trim().isEmpty()) ? "-" : t.upiId);
        tvDesc.setText((t.description == null || t.description.trim().isEmpty()) ? "-" : t.description);
    }
}
