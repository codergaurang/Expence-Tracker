// app/src/main/java/com/example/expensetracker/ui/AddCashActivity.java
package com.example.expensetracker.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.expensetracker.R;
import com.example.expensetracker.db.DbHelper;
import com.example.expensetracker.model.Transaction;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

public class AddCashActivity extends AppCompatActivity {

    private DbHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cash);

        db = new DbHelper(this);

        TextInputEditText etAmount = findViewById(R.id.etAmount);
        TextInputEditText etDesc = findViewById(R.id.etDesc);
        RadioGroup rgDir = findViewById(R.id.rgDir);
        Button btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String amtS = etAmount.getText() == null ? "" : etAmount.getText().toString().trim();
            String desc = etDesc.getText() == null ? "" : etDesc.getText().toString().trim();

            if (amtS.isEmpty()) {
                Toast.makeText(this, "Enter amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amt;
            try {
                amt = Double.parseDouble(amtS);
            } catch (Exception e) {
                Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amt <= 0) {
                Toast.makeText(this, "Amount must be > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            if (desc.isEmpty()) {
                Toast.makeText(this, "Enter description", Toast.LENGTH_SHORT).show();
                return;
            }

            String direction = (rgDir.getCheckedRadioButtonId() == R.id.rbReceived) ? "RECEIVED" : "PAID";

            Transaction t = new Transaction();
            t.amount = amt;
            t.txTimeMillis = System.currentTimeMillis();
            t.direction = direction;
            t.mode = "CASH";
            t.txnId = "CASH-" + UUID.randomUUID().toString();
            t.upiId = "";
            t.description = desc;
            t.smsUniqueId = null;
            t.rawBody = null;

            db.insertTransaction(t);
            finish();
        });
    }
}
