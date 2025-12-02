package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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

public class AddItemActivity extends AppCompatActivity {
    private static final String TAG = "AddItemActivity";

    private TextView titleTextView;
    private EditText itemNameEditText;
    private EditText itemDescriptionEditText;
    private EditText itemPriceEditText;
    private CheckBox isFreeCheckBox;
    private Button saveItemButton;
    private Button returnButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private String currentUserName;

    private String categoryId;
    private String categoryName;
    private boolean isEdit = false;
    private String itemId;
    private long originalPostedAt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        // Hide default ActionBar – we’re using a custom header
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

        titleTextView = findViewById(R.id.titleTextView);
        itemNameEditText = findViewById(R.id.itemNameEditText);
        itemDescriptionEditText = findViewById(R.id.itemDescriptionEditText);
        itemPriceEditText = findViewById(R.id.itemPriceEditText);
        isFreeCheckBox = findViewById(R.id.isFreeCheckBox);
        saveItemButton = findViewById(R.id.saveItemButton);
        returnButton = findViewById(R.id.returnButton);

        // Return button just finishes this activity
        returnButton.setOnClickListener(v -> finish());

        // Check if editing existing item
        if (getIntent().hasExtra("isEdit")) {
            isEdit = getIntent().getBooleanExtra("isEdit", false);
            itemId = getIntent().getStringExtra("itemId");

            titleTextView.setText("Edit Item");
            itemNameEditText.setText(getIntent().getStringExtra("itemName"));
            itemDescriptionEditText.setText(getIntent().getStringExtra("itemDescription"));

            boolean isFree = getIntent().getBooleanExtra("itemIsFree", false);
            isFreeCheckBox.setChecked(isFree);

            if (!isFree) {
                double price = getIntent().getDoubleExtra("itemPrice", 0);
                itemPriceEditText.setText(String.valueOf(price));
            }

            saveItemButton.setText("Update Item");

            // Load original posted date
            loadOriginalPostedDate();
        } else {
            titleTextView.setText("Add New Item to " + categoryName);
        }

        // Toggle price field based on free checkbox
        isFreeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            itemPriceEditText.setEnabled(!isChecked);
            if (isChecked) {
                itemPriceEditText.setText("");
            }
        });

        saveItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveItem();
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

    private void loadOriginalPostedDate() {
        mDatabase.child("items").child(itemId).child("postedAt")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        Long timestamp = dataSnapshot.getValue(Long.class);
                        originalPostedAt = timestamp != null ? timestamp : System.currentTimeMillis();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        originalPostedAt = System.currentTimeMillis();
                    }
                });
    }

    private void saveItem() {
        String itemName = itemNameEditText.getText().toString().trim();
        String itemDescription = itemDescriptionEditText.getText().toString().trim();
        boolean isFree = isFreeCheckBox.isChecked();

        if (TextUtils.isEmpty(itemName)) {
            itemNameEditText.setError("Item name is required");
            return;
        }

        double price = 0.0;
        if (!isFree) {
            String priceStr = itemPriceEditText.getText().toString().trim();
            if (TextUtils.isEmpty(priceStr)) {
                itemPriceEditText.setError("Price is required or check 'Free'");
                return;
            }
            try {
                price = Double.parseDouble(priceStr);
                if (price < 0) {
                    itemPriceEditText.setError("Price must be positive");
                    return;
                }
            } catch (NumberFormatException e) {
                itemPriceEditText.setError("Invalid price format");
                return;
            }
        }

        if (currentUserName == null) {
            Toast.makeText(this, "Loading user data...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isEdit) {
            // Update existing item (User Story 9)
            // Update all fields EXCEPT postedAt to preserve original creation time
            mDatabase.child("items").child(itemId).child("name").setValue(itemName);
            mDatabase.child("items").child(itemId).child("description").setValue(itemDescription);
            mDatabase.child("items").child(itemId).child("price").setValue(price);
            mDatabase.child("items").child(itemId).child("free").setValue(isFree);

            // CRITICAL: Preserve original posted time
            if (originalPostedAt > 0) {
                mDatabase.child("items").child(itemId).child("postedAt").setValue(originalPostedAt);
            }

            mDatabase.child("items").child(itemId).child("free").setValue(isFree)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(AddItemActivity.this,
                                    "Item updated successfully",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(AddItemActivity.this,
                                    "Failed to update item",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Create new item (User Story 8)
            String newItemId = mDatabase.child("items").push().getKey();

            if (newItemId != null) {
                Item item = new Item(
                        newItemId,
                        itemName,
                        itemDescription,
                        categoryId,
                        categoryName,
                        price,
                        isFree,
                        currentUserId,
                        currentUserName,
                        System.currentTimeMillis(), // ⭐ New items get current timestamp
                        "available"
                );

                mDatabase.child("items").child(newItemId).setValue(item)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(AddItemActivity.this,
                                        "Item posted successfully",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddItemActivity.this,
                                        "Failed to post item",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }
}
