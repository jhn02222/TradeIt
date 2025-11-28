package edu.uga.cs.tradeit;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ItemsActivity extends AppCompatActivity {

    private static final String TAG = "ItemsActivity";

    private ListView itemsListView;
    private FloatingActionButton addItemFab;
    private LinearLayout emptyStateLayout;

    private Button backButton;
    private TextView titleTextView;
    private TextView subtitleTextView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private String currentUserName;

    private String categoryId;
    private String categoryName;
    private List<Item> itemsList;
    private ItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_items);

        // Hide action bar if exists
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadCurrentUserName();
        }

        categoryId = getIntent().getStringExtra("categoryId");
        categoryName = getIntent().getStringExtra("categoryName");

        // Init header views
        backButton = findViewById(R.id.backButton);
        titleTextView = findViewById(R.id.titleTextView);
        subtitleTextView = findViewById(R.id.subtitleTextView);

        // Set initial header text
        titleTextView.setText(categoryName != null ? categoryName : "Items");
        subtitleTextView.setText("0 items available");

        // Back button behavior
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Initialize list / empty state / FAB
        itemsListView = findViewById(R.id.itemsListView);
        addItemFab = findViewById(R.id.addItemFab);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);

        itemsList = new ArrayList<>();
        adapter = new ItemsAdapter(this, itemsList);
        itemsListView.setAdapter(adapter);

        setupClickListeners();
        loadItems();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Update header when orientation changes
        updateHeader();
    }

    private void setupClickListeners() {
        // Click on item to view details or purchase
        itemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item selectedItem = itemsList.get(position);

                if (selectedItem.getPostedBy().equals(currentUserId)) {
                    // User's own item - show edit/delete options
                    showItemOptionsDialog(selectedItem);
                } else {
                    // Someone else's item - show purchase option
                    showPurchaseDialog(selectedItem);
                }
            }
        });

        // FAB (Floating Action Button)
        addItemFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ItemsActivity.this, AddItemActivity.class);
                intent.putExtra("categoryId", categoryId);
                intent.putExtra("categoryName", categoryName);
                startActivity(intent);
            }
        });
    }

    private void loadCurrentUserName() {
        mDatabase.child("users").child(currentUserId).child("displayName")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        currentUserName = dataSnapshot.getValue(String.class);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        currentUserName = "Unknown";
                    }
                });
    }

    private void loadItems() {
        Query query = mDatabase.child("items")
                .orderByChild("categoryId")
                .equalTo(categoryId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                itemsList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null && "available".equals(item.getStatus())) {
                        itemsList.add(item);
                    }
                }

                // Sort from newest to oldest
                Collections.sort(itemsList, new Comparator<Item>() {
                    @Override
                    public int compare(Item i1, Item i2) {
                        return Long.compare(i2.getPostedAt(), i1.getPostedAt());
                    }
                });

                adapter.notifyDataSetChanged();
                updateHeader();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load items", databaseError.toException());
                Toast.makeText(ItemsActivity.this,
                        "Failed to load items",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Updates the custom header (title + subtitle) based on orientation and item count.
     */
    private void updateHeader() {
        int count = itemsList.size();
        int orientation = getResources().getConfiguration().orientation;

        if (categoryName == null) {
            categoryName = "Items";
        }

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Compact single-line title in landscape
            String title = categoryName + " (" + count + (count == 1 ? " item)" : " items)");
            titleTextView.setText(title);
            subtitleTextView.setVisibility(View.GONE);
        } else {
            // Title + subtitle in portrait
            titleTextView.setText(categoryName);
            String subtitle = (count == 1)
                    ? "1 item available"
                    : count + " items available";
            subtitleTextView.setText(subtitle);
            subtitleTextView.setVisibility(View.VISIBLE);
        }
    }

    private void updateEmptyState() {
        if (itemsList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            itemsListView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            itemsListView.setVisibility(View.VISIBLE);
        }
    }

    private void showItemOptionsDialog(final Item item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getName());
        builder.setItems(new CharSequence[]{"Edit", "Delete"},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            // Edit item
                            Intent intent = new Intent(ItemsActivity.this, AddItemActivity.class);
                            intent.putExtra("isEdit", true);
                            intent.putExtra("itemId", item.getItemId());
                            intent.putExtra("itemName", item.getName());
                            intent.putExtra("itemDescription", item.getDescription());
                            intent.putExtra("itemPrice", item.getPrice());
                            intent.putExtra("itemIsFree", item.isFree());
                            intent.putExtra("categoryId", categoryId);
                            intent.putExtra("categoryName", categoryName);
                            startActivity(intent);
                        } else {
                            // Delete item
                            deleteItem(item);
                        }
                    }
                });
        builder.show();
    }

    private void deleteItem(Item item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"" + item.getName() + "\"?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mDatabase.child("items").child(item.getItemId()).removeValue()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(ItemsActivity.this,
                                                "Item deleted",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(ItemsActivity.this,
                                                "Failed to delete item",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPurchaseDialog(final Item item) {
        String message;
        if (item.isFree()) {
            message = "Do you want to accept this free item?";
        } else {
            message = "Do you want to purchase this item for $" +
                    String.format("%.2f", item.getPrice()) + "?";
        }

        new AlertDialog.Builder(this)
                .setTitle(item.getName())
                .setMessage(message)
                .setPositiveButton(item.isFree() ? "Accept" : "Purchase",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                createTransaction(item);
                            }
                        })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void createTransaction(Item item) {
        // Create transaction ID
        String transactionId = mDatabase.child("transactions").push().getKey();

        if (transactionId == null || currentUserName == null) {
            Toast.makeText(this, "Failed to create transaction",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Create transaction object
        Transaction transaction = new Transaction(
                transactionId,
                item.getItemId(),
                item.getName(),
                item.getCategoryName(),
                item.getPostedBy(),
                item.getPostedByName(),
                currentUserId,
                currentUserName,
                item.getPrice(),
                System.currentTimeMillis(),
                "pending",
                false,
                false,
                0
        );

        // Save transaction
        mDatabase.child("transactions").child(transactionId).setValue(transaction)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update item status to pending
                        mDatabase.child("items").child(item.getItemId())
                                .child("status").setValue("pending");

                        Toast.makeText(ItemsActivity.this,
                                "Transaction created! Seller has been notified.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(ItemsActivity.this,
                                "Failed to create transaction",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}