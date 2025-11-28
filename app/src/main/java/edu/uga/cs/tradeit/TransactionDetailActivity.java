package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import edu.uga.cs.tradeit.Transaction;

public class TransactionDetailActivity extends AppCompatActivity {
    private static final String TAG = "TransactionDetailActivity";

    private TextView itemNameTextView;
    private TextView categoryTextView;
    private TextView priceTextView;
    private TextView sellerTextView;
    private TextView buyerTextView;
    private TextView dateTextView;
    private TextView statusTextView;
    private TextView confirmationStatusTextView;
    private Button confirmButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    private String transactionId;
    private String userRole; // "buyer", "seller", or "completed"
    private Transaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_detail);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        transactionId = getIntent().getStringExtra("transactionId");
        userRole = getIntent().getStringExtra("userRole");

        itemNameTextView = findViewById(R.id.itemNameTextView);
        categoryTextView = findViewById(R.id.categoryTextView);
        priceTextView = findViewById(R.id.priceTextView);
        sellerTextView = findViewById(R.id.sellerTextView);
        buyerTextView = findViewById(R.id.buyerTextView);
        dateTextView = findViewById(R.id.dateTextView);
        statusTextView = findViewById(R.id.statusTextView);
        confirmationStatusTextView = findViewById(R.id.confirmationStatusTextView);
        confirmButton = findViewById(R.id.confirmButton);
        Button returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(v -> finish());
        loadTransactionDetails();

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmTransaction();
            }
        });
    }

    private void loadTransactionDetails() {
        mDatabase.child("transactions").child(transactionId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        transaction = dataSnapshot.getValue(Transaction.class);

                        if (transaction != null) {
                            displayTransactionDetails();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Failed to load transaction", databaseError.toException());
                        Toast.makeText(TransactionDetailActivity.this,
                                "Failed to load transaction details",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayTransactionDetails() {
        itemNameTextView.setText("Item: " + transaction.getItemName());
        categoryTextView.setText("Category: " + transaction.getCategoryName());

        if (transaction.getPrice() > 0) {
            priceTextView.setText("Price: $" + String.format("%.2f", transaction.getPrice()));
        } else {
            priceTextView.setText("Price: FREE");
        }

        sellerTextView.setText("Seller: " + transaction.getSellerName());
        buyerTextView.setText("Buyer: " + transaction.getBuyerName());

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.US);
        String dateStr = sdf.format(new Date(transaction.getCreatedAt()));
        dateTextView.setText("Date: " + dateStr);

        statusTextView.setText("Status: " + transaction.getStatus().toUpperCase());

        // Show confirmation status
        if ("pending".equals(transaction.getStatus())) {
            String confirmStatus = "Confirmations:\n";
            confirmStatus += "Seller: " + (transaction.isSellerConfirmed() ? "✓ Confirmed" : "Pending") + "\n";
            confirmStatus += "Buyer: " + (transaction.isBuyerConfirmed() ? "✓ Confirmed" : "Pending");
            confirmationStatusTextView.setText(confirmStatus);
            confirmationStatusTextView.setVisibility(View.VISIBLE);

            // Show/hide confirm button based on user role and confirmation status
            if ("buyer".equals(userRole) && !transaction.isBuyerConfirmed()) {
                confirmButton.setVisibility(View.VISIBLE);
                confirmButton.setText("Confirm Receipt");
            } else if ("seller".equals(userRole) && !transaction.isSellerConfirmed()) {
                confirmButton.setVisibility(View.VISIBLE);
                confirmButton.setText("Confirm Delivery");
            } else {
                confirmButton.setVisibility(View.GONE);
            }
        } else if ("completed".equals(transaction.getStatus())) {
            String completedDate = sdf.format(new Date(transaction.getCompletedAt()));
            confirmationStatusTextView.setText("Completed on: " + completedDate);
            confirmationStatusTextView.setVisibility(View.VISIBLE);
            confirmButton.setVisibility(View.GONE);
        } else {
            confirmationStatusTextView.setVisibility(View.GONE);
            confirmButton.setVisibility(View.GONE);
        }
    }

    private void confirmTransaction() {
        if (transaction == null) return;

        boolean isBuyer = "buyer".equals(userRole);
        boolean isSeller = "seller".equals(userRole);

        // Update confirmation status
        if (isBuyer) {
            mDatabase.child("transactions").child(transactionId)
                    .child("buyerConfirmed").setValue(true);
        } else if (isSeller) {
            mDatabase.child("transactions").child(transactionId)
                    .child("sellerConfirmed").setValue(true);
        }

        // Check if both have confirmed
        boolean bothConfirmed = false;
        if (isBuyer) {
            bothConfirmed = transaction.isSellerConfirmed();
        } else if (isSeller) {
            bothConfirmed = transaction.isBuyerConfirmed();
        }

        if (bothConfirmed) {
            // Mark transaction as completed
            long completedTime = System.currentTimeMillis();
            mDatabase.child("transactions").child(transactionId)
                    .child("status").setValue("completed");
            mDatabase.child("transactions").child(transactionId)
                    .child("completedAt").setValue(completedTime);

            // Update item status to sold
            mDatabase.child("items").child(transaction.getItemId())
                    .child("status").setValue("sold");

            Toast.makeText(this,
                    "Transaction completed! Both parties have confirmed.",
                    Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this,
                    "Confirmed! Waiting for the other party to confirm.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}