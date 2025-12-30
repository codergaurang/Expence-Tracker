// app/src/main/java/com/example/expensetracker/ui/DescriptionDialogFragment.java
package com.example.expensetracker.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.expensetracker.R;
import com.example.expensetracker.db.DbHelper;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.util.TimeFormat;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Locale;

public class DescriptionDialogFragment extends DialogFragment {

    public interface Callback {
        void onDescriptionSaved();
    }

    private static final String ARG_TX_ID = "arg_tx_id";

    public static DescriptionDialogFragment newInstance(long txId) {
        DescriptionDialogFragment f = new DescriptionDialogFragment();
        Bundle b = new Bundle();
        b.putLong(ARG_TX_ID, txId);
        f.setArguments(b);
        f.setCancelable(false);
        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View v = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_description, null);

        TextView tvTxSummary = v.findViewById(R.id.tvTxSummary);
        TextInputEditText et = v.findViewById(R.id.etDescription);

        long txId = requireArguments().getLong(ARG_TX_ID, -1);
        DbHelper db = new DbHelper(requireContext());
        Transaction t = db.getById(txId);

        if (t != null) {
            String amountStr = "₹ " + String.format(Locale.getDefault(), "%.2f", t.amount);
            String when = TimeFormat.formatForUi(t.txTimeMillis);
            tvTxSummary.setText(amountStr + " • " + t.direction + " • " + when);
        } else {
            tvTxSummary.setText("Transaction: -");
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add description (required)")
                .setView(v)
                .setCancelable(false)
                .setPositiveButton("Save", null)
                .create();

        dialog.setCanceledOnTouchOutside(false);

        dialog.setOnShowListener(d -> {
            Button btn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btn.setEnabled(false);

            et.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    btn.setEnabled(s != null && s.toString().trim().length() > 0);
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            btn.setOnClickListener(x -> {
                String desc = et.getText() == null ? "" : et.getText().toString().trim();
                if (desc.isEmpty()) return;

                long id = requireArguments().getLong(ARG_TX_ID, -1);
                DbHelper db2 = new DbHelper(requireContext());
                db2.updateDescription(id, desc);

                if (getActivity() instanceof Callback) {
                    ((Callback) getActivity()).onDescriptionSaved();
                }
                dismissAllowingStateLoss();
            });
        });

        return dialog;
    }
}
