// app/src/main/java/com/example/expensetracker/adapter/TransactionAdapter.java
package com.example.expensetracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.expensetracker.R;
import com.example.expensetracker.model.Transaction;
import com.example.expensetracker.util.TimeFormat;

import java.util.ArrayList;
import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.VH> {

    public interface OnItemClick {
        void onClick(Transaction t);
    }

    private final List<Transaction> items = new ArrayList<>();
    private final OnItemClick listener;

    public TransactionAdapter(OnItemClick listener) {
        this.listener = listener;
    }

    public void setItems(List<Transaction> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_transaction, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Transaction t = items.get(position);

        h.tvAmount.setText("₹ " + String.format("%.2f", t.amount));
        h.tvDateTime.setText(TimeFormat.formatForUi(t.txTimeMillis));
        h.tvDir.setText(t.direction);
        h.tvMode.setText(t.mode);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(t);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvAmount, tvDateTime, tvDir, tvMode;

        VH(@NonNull View itemView) {
            super(itemView);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDir = itemView.findViewById(R.id.tvDir);
            tvMode = itemView.findViewById(R.id.tvMode);
        }
    }
}
