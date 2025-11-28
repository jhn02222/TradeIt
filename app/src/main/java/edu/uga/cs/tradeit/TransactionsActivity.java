package edu.uga.cs.tradeit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TabHost;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TransactionsActivity extends AppCompatActivity {
    private static final String TAG = "TransactionsActivity";

    private TabHost tabHost;
    private ListView pendingBuysListView;
    private ListView pendingSalesListView;
    private ListView completedListView;

    private LinearLayout pendingBuysEmptyView;
    private LinearLayout pendingSalesEmptyView;
    private LinearLayout completedEmptyView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    private List<Transaction> pendingBuysList;
    private List<Transaction> pendingSalesList;
    private List<Transaction> completedList;

    private TransactionsAdapter pendingBuysAdapter;
    private TransactionsAdapter pendingSalesAdapter;
    private TransactionsAdapter completedAdapter;

    private TextView titleTextView;
    private Button returnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions);

        // Using custom header instead of ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Header views
        titleTextView = findViewById(R.id.titleTextView);
        returnButton = findViewById(R.id.returnButton);

        titleTextView.setText("Activity");
        returnButton.setOnClickListener(v -> finish());

        // Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // ListViews
        pendingBuysListView = findViewById(R.id.pendingBuysListView);
        pendingSalesListView = findViewById(R.id.pendingSalesListView);
        completedListView = findViewById(R.id.completedListView);

        // Empty views
        pendingBuysEmptyView = findViewById(R.id.pendingBuysEmptyView);
        pendingSalesEmptyView = findViewById(R.id.pendingSalesEmptyView);
        completedEmptyView = findViewById(R.id.completedEmptyView);

        // TabHost setup
        tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        // Tab 1: Pending Buys (using container ID)
        TabHost.TabSpec spec1 = tabHost.newTabSpec("PendingBuys");
        spec1.setIndicator("Pending Buys");
        spec1.setContent(R.id.pendingBuysContainer);
        tabHost.addTab(spec1);

        // Tab 2: Pending Sales (using container ID)
        TabHost.TabSpec spec2 = tabHost.newTabSpec("PendingSales");
        spec2.setIndicator("Pending Sales");
        spec2.setContent(R.id.pendingSalesContainer);
        tabHost.addTab(spec2);

        // Tab 3: Completed (using container ID)
        TabHost.TabSpec spec3 = tabHost.newTabSpec("Completed");
        spec3.setIndicator("Completed");
        spec3.setContent(R.id.completedContainer);
        tabHost.addTab(spec3);

        // Set initial tab to Pending Buys (tab 0)
        tabHost.setCurrentTab(0);

        // Lists + adapters
        pendingBuysList = new ArrayList<>();
        pendingSalesList = new ArrayList<>();
        completedList = new ArrayList<>();

        pendingBuysAdapter = new TransactionsAdapter(this, pendingBuysList, "buyer");
        pendingSalesAdapter = new TransactionsAdapter(this, pendingSalesList, "seller");
        completedAdapter = new TransactionsAdapter(this, completedList, "completed");

        pendingBuysListView.setAdapter(pendingBuysAdapter);
        pendingSalesListView.setAdapter(pendingSalesAdapter);
        completedListView.setAdapter(completedAdapter);

        // Click listeners
        pendingBuysListView.setOnItemClickListener((parent, view, position, id) -> {
            Transaction transaction = pendingBuysList.get(position);
            openTransactionDetail(transaction, "buyer");
        });

        pendingSalesListView.setOnItemClickListener((parent, view, position, id) -> {
            Transaction transaction = pendingSalesList.get(position);
            openTransactionDetail(transaction, "seller");
        });

        completedListView.setOnItemClickListener((parent, view, position, id) -> {
            Transaction transaction = completedList.get(position);
            openTransactionDetail(transaction, "completed");
        });

        // Load data
        loadTransactions();
    }

    private void loadTransactions() {
        mDatabase.child("transactions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                pendingBuysList.clear();
                pendingSalesList.clear();
                completedList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Transaction transaction = snapshot.getValue(Transaction.class);
                    if (transaction != null && currentUserId != null) {
                        if ("pending".equals(transaction.getStatus())) {
                            if (transaction.getBuyerId().equals(currentUserId)) {
                                pendingBuysList.add(transaction);
                            }
                            if (transaction.getSellerId().equals(currentUserId)) {
                                pendingSalesList.add(transaction);
                            }
                        } else if ("completed".equals(transaction.getStatus())) {
                            if (transaction.getBuyerId().equals(currentUserId)
                                    || transaction.getSellerId().equals(currentUserId)) {
                                completedList.add(transaction);
                            }
                        }
                    }
                }

                // Sort by createdAt
                Comparator<Transaction> createdComparator = (t1, t2) ->
                        Long.compare(t2.getCreatedAt(), t1.getCreatedAt());

                Collections.sort(pendingBuysList, createdComparator);
                Collections.sort(pendingSalesList, createdComparator);

                // Completed: sort by completedAt if present, else createdAt
                Collections.sort(completedList, (t1, t2) -> {
                    long time1 = t1.getCompletedAt() > 0 ? t1.getCompletedAt() : t1.getCreatedAt();
                    long time2 = t2.getCompletedAt() > 0 ? t2.getCompletedAt() : t2.getCreatedAt();
                    return Long.compare(time2, time1);
                });

                pendingBuysAdapter.notifyDataSetChanged();
                pendingSalesAdapter.notifyDataSetChanged();
                completedAdapter.notifyDataSetChanged();

                // Update empty views
                updateEmptyViews();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load transactions", databaseError.toException());
                Toast.makeText(TransactionsActivity.this,
                        "Failed to load transactions",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateEmptyViews() {
        // Pending Buys
        if (pendingBuysList.isEmpty()) {
            pendingBuysListView.setVisibility(View.GONE);
            pendingBuysEmptyView.setVisibility(View.VISIBLE);
        } else {
            pendingBuysListView.setVisibility(View.VISIBLE);
            pendingBuysEmptyView.setVisibility(View.GONE);
        }

        // Pending Sales
        if (pendingSalesList.isEmpty()) {
            pendingSalesListView.setVisibility(View.GONE);
            pendingSalesEmptyView.setVisibility(View.VISIBLE);
        } else {
            pendingSalesListView.setVisibility(View.VISIBLE);
            pendingSalesEmptyView.setVisibility(View.GONE);
        }

        // Completed
        if (completedList.isEmpty()) {
            completedListView.setVisibility(View.GONE);
            completedEmptyView.setVisibility(View.VISIBLE);
        } else {
            completedListView.setVisibility(View.VISIBLE);
            completedEmptyView.setVisibility(View.GONE);
        }
    }

    private void openTransactionDetail(Transaction transaction, String userRole) {
        Intent intent = new Intent(this, TransactionDetailActivity.class);
        intent.putExtra("transactionId", transaction.getTransactionId());
        intent.putExtra("userRole", userRole);
        startActivity(intent);
    }
}