package edu.uga.cs.tradeit;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import edu.uga.cs.tradeit.Category;

public class AddCategoryActivity extends AppCompatActivity {
    private static final String TAG = "AddCategoryActivity";

    private TextView titleTextView;
    private EditText categoryNameEditText;
    private Button saveCategoryButton;
    private Button cancelButton;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;

    private boolean isEdit = false;
    private String categoryId;
    private String originalCategoryName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        titleTextView = findViewById(R.id.titleTextView);
        categoryNameEditText = findViewById(R.id.categoryNameEditText);
        saveCategoryButton = findViewById(R.id.saveCategoryButton);
        cancelButton = findViewById(R.id.cancelButton);

        // Check if editing existing category
        if (getIntent().hasExtra("isEdit")) {
            isEdit = getIntent().getBooleanExtra("isEdit", false);
            categoryId = getIntent().getStringExtra("categoryId");
            originalCategoryName = getIntent().getStringExtra("categoryName");

            titleTextView.setText("Edit Category");
            categoryNameEditText.setText(originalCategoryName);
            saveCategoryButton.setText("Update Category");
        }

        saveCategoryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCategory();
            }
        });

        // Add cancel button listener
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        // Add back button listener if it exists
        View backButton = findViewById(R.id.backButton);
        if (backButton != null) {
            backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    private void saveCategory() {
        String categoryName = categoryNameEditText.getText().toString().trim();

        if (TextUtils.isEmpty(categoryName)) {
            categoryNameEditText.setError("Category name is required");
            return;
        }

        // Disable button to prevent double submission
        saveCategoryButton.setEnabled(false);

        // Check for duplicate category name
        checkDuplicateCategory(categoryName);
    }

    private void checkDuplicateCategory(final String categoryName) {
        // Query to check if category name already exists (case-insensitive)
        Query query = mDatabase.child("categories")
                .orderByChild("name");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean isDuplicate = false;

                // Check all categories for case-insensitive match
                for (DataSnapshot categorySnapshot : dataSnapshot.getChildren()) {
                    Category category = categorySnapshot.getValue(Category.class);
                    if (category != null) {
                        String existingName = category.getName();

                        // If editing, ignore the current category being edited
                        if (isEdit && categorySnapshot.getKey().equals(categoryId)) {
                            continue;
                        }

                        // Case-insensitive comparison
                        if (existingName != null && existingName.equalsIgnoreCase(categoryName)) {
                            isDuplicate = true;
                            break;
                        }
                    }
                }

                if (isDuplicate) {
                    // Re-enable button
                    saveCategoryButton.setEnabled(true);

                    // Show error message
                    categoryNameEditText.setError("Category already exists");
                    Toast.makeText(AddCategoryActivity.this,
                            "A category with this name already exists",
                            Toast.LENGTH_LONG).show();
                } else {
                    // No duplicate found, proceed with save
                    proceedWithSave(categoryName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Re-enable button
                saveCategoryButton.setEnabled(true);

                Log.e(TAG, "Database error: " + databaseError.getMessage());
                Toast.makeText(AddCategoryActivity.this,
                        "Error checking category: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void proceedWithSave(String categoryName) {
        if (isEdit) {
            // Update existing category
            mDatabase.child("categories").child(categoryId).child("name")
                    .setValue(categoryName)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Update timestamp
                            mDatabase.child("categories").child(categoryId)
                                    .child("createdAt").setValue(System.currentTimeMillis());

                            Toast.makeText(AddCategoryActivity.this,
                                    "Category updated successfully",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            saveCategoryButton.setEnabled(true);
                            Toast.makeText(AddCategoryActivity.this,
                                    "Failed to update category",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Create new category
            String newCategoryId = mDatabase.child("categories").push().getKey();

            if (newCategoryId != null) {
                Category category = new Category(
                        newCategoryId,
                        categoryName,
                        currentUserId,
                        System.currentTimeMillis()
                );

                mDatabase.child("categories").child(newCategoryId).setValue(category)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(AddCategoryActivity.this,
                                        "Category created successfully",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                saveCategoryButton.setEnabled(true);
                                Toast.makeText(AddCategoryActivity.this,
                                        "Failed to create category",
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                saveCategoryButton.setEnabled(true);
                Toast.makeText(AddCategoryActivity.this,
                        "Failed to generate category ID",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}