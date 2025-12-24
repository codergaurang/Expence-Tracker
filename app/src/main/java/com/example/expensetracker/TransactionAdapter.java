package com.example.expensetracker;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;

    public TransactionAdapter(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);

        // Amount with rupee sign
        holder.amountText.setText("₹" + String.format("%.2f", transaction.getAmount()));

        // Date & Time
        String dateTime = transaction.getDateTime();
        if (dateTime.length() > 16) {
            dateTime = dateTime.substring(0, 16);
        }
        holder.dateTimeText.setText(dateTime);

        // Type with color coding
        holder.typeText.setText(transaction.getType());

        // Mode
        holder.modeText.setText(transaction.getPaymentMode());

        if ("RECEIVED".equals(transaction.getType())) {
            holder.amountText.setTextColor(Color.parseColor("#27AE60")); // Green
            holder.typeText.setBackgroundColor(Color.parseColor("#E8F8F5"));
            holder.typeText.setTextColor(Color.parseColor("#27AE60"));
        } else {
            holder.amountText.setTextColor(Color.parseColor("#C0392B")); // Red
            holder.typeText.setBackgroundColor(Color.parseColor("#FADBD8"));
            holder.typeText.setTextColor(Color.parseColor("#C0392B"));
        }
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountText, dateTimeText, typeText, modeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            amountText = itemView.findViewById(R.id.amountText);
            dateTimeText = itemView.findViewById(R.id.dateTimeText);
            typeText = itemView.findViewById(R.id.typeText);
            modeText = itemView.findViewById(R.id.modeText);
        }
    }
}
