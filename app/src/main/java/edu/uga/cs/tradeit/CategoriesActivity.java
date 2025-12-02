// ============================================================================
// FILE: CategoriesActivity.java (REDESIGNED - WITH BOTTOM BUTTONS)
// LOCATION: app/src/main/java/edu/uga/cs/tradeit/CategoriesActivity.java
// ============================================================================

package edu.uga.cs.tradeit;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
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

public class CategoriesActivity extends AppCompatActivity {
    private static final String TAG = "CategoriesActivity";

    private ListView categoriesListView;
    private Button addButton;
    private Button btnMyItems;
    private Button btnTransactions;
    private Button btnLogout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private List<Category> categoriesList;
    private CategoriesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_categories);

        // Hide ActionBar for cleaner look
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUserId = currentUser.getUid();

        categoriesListView = findViewById(R.id.categoriesListView);
        addButton = findViewById(R.id.addButton);
        btnMyItems = findViewById(R.id.btnMyItems);
        btnTransactions = findViewById(R.id.btnTransactions);
        btnLogout = findViewById(R.id.btnLogout);

        categoriesList = new ArrayList<>();
        adapter = new CategoriesAdapter(this, categoriesList);
        categoriesListView.setAdapter(adapter);


        loadCategories();

        // Click category to view items
        categoriesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Category selectedCategory = categoriesList.get(position);
                Intent intent = new Intent(CategoriesActivity.this, ItemsActivity.class);
                intent.putExtra("categoryId", selectedCategory.getCategoryId());
                intent.putExtra("categoryName", selectedCategory.getName());
                startActivity(intent);
            }
        });

        // Long press for options
        categoriesListView.setOnItemLongClickListener(
                new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                                   int position, long id) {
                        Category selectedCategory = categoriesList.get(position);
                        showCategoryOptionsDialog(selectedCategory);
                        return true;
                    }
                });

        // Add button
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CategoriesActivity.this, AddCategoryActivity.class));
            }
        });

        // Bottom navigation buttons
        btnMyItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CategoriesActivity.this, MyItemsActivity.class));
            }
        });

        btnTransactions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CategoriesActivity.this, TransactionsActivity.class));
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(CategoriesActivity.this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Logout", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mAuth.signOut();
                                startActivity(new Intent(CategoriesActivity.this, LoginActivity.class));
                                finish();
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
    }

    private void loadCategories() {
        mDatabase.child("categories").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                categoriesList.clear();
                // Remove: categoryNames.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Category category = snapshot.getValue(Category.class);
                    if (category != null) {
                        categoriesList.add(category);
                    }
                }

                Collections.sort(categoriesList, new Comparator<Category>() {
                    @Override
                    public int compare(Category c1, Category c2) {
                        return c1.getName().compareToIgnoreCase(c2.getName());
                    }
                });

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (mAuth.getCurrentUser() == null) return;
                Log.e(TAG, "Failed to load categories", databaseError.toException());
                Toast.makeText(CategoriesActivity.this,
                        "Failed to load categories",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCategoryOptionsDialog(final Category category) {
        if (!category.getCreatedBy().equals(currentUserId)) {
            Toast.makeText(this, "You can only edit/delete categories you created",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(category.getName());
        builder.setItems(new CharSequence[]{"Edit", "Delete"},
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) {
                            editCategory(category);
                        } else {
                            deleteCategory(category);
                        }
                    }
                });
        builder.show();
    }

    private void editCategory(Category category) {
        // Check if category is empty before allowing edit
        checkIfCategoryEmpty(category.getCategoryId(), new CategoryEmptyCallback() {
            @Override
            public void onResult(boolean isEmpty) {
                if (isEmpty) {
                    // Category is empty, allow editing
                    Intent intent = new Intent(CategoriesActivity.this, AddCategoryActivity.class);
                    intent.putExtra("categoryId", category.getCategoryId());
                    intent.putExtra("categoryName", category.getName());
                    intent.putExtra("isEdit", true);
                    startActivity(intent);
                } else {
                    // Category has items, prevent editing
                    Toast.makeText(CategoriesActivity.this,
                            "Cannot edit category with items in it",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void deleteCategory(Category category) {
        checkIfCategoryEmpty(category.getCategoryId(), new CategoryEmptyCallback() {
            @Override
            public void onResult(boolean isEmpty) {
                if (isEmpty) {
                    new AlertDialog.Builder(CategoriesActivity.this)
                            .setTitle("Delete Category")
                            .setMessage("Are you sure you want to delete \"" +
                                    category.getName() + "\"?")
                            .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mDatabase.child("categories")
                                            .child(category.getCategoryId())
                                            .removeValue()
                                            .addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Toast.makeText(CategoriesActivity.this,
                                                            "Category deleted",
                                                            Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(CategoriesActivity.this,
                                                            "Failed to delete category",
                                                            Toast.LENGTH_SHORT).show();
                                                }
                                            });
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                } else {
                    Toast.makeText(CategoriesActivity.this,
                            "Cannot delete category with items in it",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkIfCategoryEmpty(String categoryId, CategoryEmptyCallback callback) {
        Query query = mDatabase.child("items")
                .orderByChild("categoryId")
                .equalTo(categoryId);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean hasAvailableItems = false;

                // Check if there are any AVAILABLE items in this category
                for (DataSnapshot itemSnapshot : dataSnapshot.getChildren()) {
                    Item item = itemSnapshot.getValue(Item.class);
                    if (item != null && "available".equals(item.getStatus())) {
                        hasAvailableItems = true;
                        break;
                    }
                }

                // Category is "empty" if it has no available items
                callback.onResult(!hasAvailableItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onResult(false);
            }
        });
    }

    interface CategoryEmptyCallback {
        void onResult(boolean isEmpty);
    }
}