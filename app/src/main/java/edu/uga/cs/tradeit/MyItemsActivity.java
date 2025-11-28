package edu.uga.cs.tradeit;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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

public class MyItemsActivity extends AppCompatActivity {
    private static final String TAG = "MyItemsActivity";

    private ListView myItemsListView;
    private LinearLayout emptyStateLayout;
    private Button returnButton;
    private TextView titleTextView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    private List<Item> myItemsList;
    private ItemsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_items);

        // If there is an ActionBar from the theme, hide it since we're using a custom header
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // Header views
        titleTextView = findViewById(R.id.titleTextView);
        returnButton = findViewById(R.id.returnButton);

        // Set title text (optional; matches XML default)
        titleTextView.setText("My Items");

        // Return button goes back to previous screen
        returnButton.setOnClickListener(v -> finish());

        // Empty state + list
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
        myItemsListView = findViewById(R.id.myItemsListView);

        myItemsList = new ArrayList<>();
        adapter = new ItemsAdapter(this, myItemsList);
        myItemsListView.setAdapter(adapter);

        loadMyItems();

        myItemsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Item selectedItem = myItemsList.get(position);
                showItemOptionsDialog(selectedItem);
            }
        });
    }

    private void loadMyItems() {
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            updateEmptyState();
            return;
        }

        Query query = mDatabase.child("items")
                .orderByChild("postedBy")
                .equalTo(currentUserId);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                myItemsList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        myItemsList.add(item);
                    }
                }

                // Sort from newest to oldest
                Collections.sort(myItemsList, new Comparator<Item>() {
                    @Override
                    public int compare(Item i1, Item i2) {
                        return Long.compare(i2.getPostedAt(), i1.getPostedAt());
                    }
                });

                adapter.notifyDataSetChanged();
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Failed to load my items", databaseError.toException());
                Toast.makeText(MyItemsActivity.this,
                        "Failed to load your items",
                        Toast.LENGTH_SHORT).show();
                updateEmptyState();
            }
        });
    }

    private void updateEmptyState() {
        if (myItemsList == null || myItemsList.isEmpty()) {
            emptyStateLayout.setVisibility(View.VISIBLE);
            myItemsListView.setVisibility(View.GONE);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            myItemsListView.setVisibility(View.VISIBLE);
        }
    }

    private void showItemOptionsDialog(final Item item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getName() + " - " + item.getStatus());

        if ("available".equals(item.getStatus())) {
            builder.setItems(new CharSequence[]{"Edit", "Delete"},
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (which == 0) {
                                // Edit item
                                Intent intent = new Intent(MyItemsActivity.this, AddItemActivity.class);
                                intent.putExtra("isEdit", true);
                                intent.putExtra("itemId", item.getItemId());
                                intent.putExtra("itemName", item.getName());
                                intent.putExtra("itemDescription", item.getDescription());
                                intent.putExtra("itemPrice", item.getPrice());
                                intent.putExtra("itemIsFree", item.isFree());
                                intent.putExtra("categoryId", item.getCategoryId());
                                intent.putExtra("categoryName", item.getCategoryName());
                                startActivity(intent);
                            } else {
                                // Delete item
                                deleteItem(item);
                            }
                        }
                    });
        } else {
            builder.setMessage("This item is currently in a " + item.getStatus() + " transaction.");
            builder.setPositiveButton("OK", null);
        }

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
                                        Toast.makeText(MyItemsActivity.this,
                                                "Item deleted",
                                                Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(MyItemsActivity.this,
                                                "Failed to delete item",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

}
