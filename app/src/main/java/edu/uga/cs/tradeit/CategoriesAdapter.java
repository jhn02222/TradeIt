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

public class CategoriesAdapter extends ArrayAdapter<Category> {
    private Context context;
    private List<Category> categories;

    public CategoriesAdapter(Context context, List<Category> categories) {
        super(context, 0, categories);
        this.context = context;
        this.categories = categories;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Category category = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.category_list_item, parent, false);
        }

        TextView categoryNameTextView = convertView.findViewById(R.id.categoryNameTextView);
        TextView categoryDateTextView = convertView.findViewById(R.id.categoryDateTextView);

        if (category != null) {
            categoryNameTextView.setText(category.getName());

            // Format and display creation/update time
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US);
            String dateStr = sdf.format(new Date(category.getCreatedAt()));
            categoryDateTextView.setText("Created: " + dateStr);
        }

        return convertView;
    }
}