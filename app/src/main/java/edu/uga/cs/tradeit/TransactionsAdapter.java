package edu.uga.cs.tradeit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import edu.uga.cs.tradeit.Transaction;

public class TransactionsAdapter extends ArrayAdapter<Transaction> {
    private Context context;
    private List<Transaction> transactions;
    private String viewType; // "buyer", "seller", or "completed"

    public TransactionsAdapter(Context context, List<Transaction> transactions, String viewType) {
        super(context, 0, transactions);
        this.context = context;
        this.transactions = transactions;
        this.viewType = viewType;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Transaction transaction = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.transaction_list_item, parent, false);
        }

        TextView itemNameTextView = convertView.findViewById(R.id.transactionItemNameTextView);
        TextView priceTextView = convertView.findViewById(R.id.transactionPriceTextView);
        TextView otherPartyTextView = convertView.findViewById(R.id.transactionOtherPartyTextView);
        TextView dateTextView = convertView.findViewById(R.id.transactionDateTextView);
        TextView statusTextView = convertView.findViewById(R.id.transactionStatusTextView);

        if (transaction != null) {
            itemNameTextView.setText(transaction.getItemName());

            if (transaction.getPrice() > 0) {
                priceTextView.setText("$" + String.format("%.2f", transaction.getPrice()));
            } else {
                priceTextView.setText("FREE");
            }

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            long displayTime = transaction.getCreatedAt();

            if ("buyer".equals(viewType)) {
                otherPartyTextView.setText("Seller: " + transaction.getSellerName());
            } else if ("seller".equals(viewType)) {
                otherPartyTextView.setText("Buyer: " + transaction.getBuyerName());
            } else if ("completed".equals(viewType)) {
                displayTime = transaction.getCompletedAt() > 0 ?
                        transaction.getCompletedAt() : transaction.getCreatedAt();
                // Show role in completed view
                String role = transaction.getBuyerId().equals(
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())
                        ? "Bought from: " : "Sold to: ";
                String otherParty = transaction.getBuyerId().equals(
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())
                        ? transaction.getSellerName() : transaction.getBuyerName();
                otherPartyTextView.setText(role + otherParty);
            }

            String dateStr = sdf.format(new Date(displayTime));
            dateTextView.setText(dateStr);

            if ("pending".equals(transaction.getStatus())) {
                String confirmStatus = "";
                if ("buyer".equals(viewType)) {
                    confirmStatus = transaction.isBuyerConfirmed() ?
                            "✓ You confirmed" : "Awaiting your confirmation";
                } else if ("seller".equals(viewType)) {
                    confirmStatus = transaction.isSellerConfirmed() ?
                            "✓ You confirmed" : "Awaiting your confirmation";
                }
                statusTextView.setText(confirmStatus);
                statusTextView.setVisibility(View.VISIBLE);
            } else if ("completed".equals(transaction.getStatus())) {
                statusTextView.setText("✓ Completed");
                statusTextView.setVisibility(View.VISIBLE);
            } else {
                statusTextView.setVisibility(View.GONE);
            }
        }

        return convertView;
    }
}