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
import java.util.concurrent.TimeUnit;

public class ItemsAdapter extends ArrayAdapter<Item> {
    private Context context;
    private List<Item> items;

    public ItemsAdapter(Context context, List<Item> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Item item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(
                    R.layout.item_list_item, parent, false);
        }

        TextView itemNameTextView = convertView.findViewById(R.id.itemNameTextView);
        TextView itemDescriptionTextView = convertView.findViewById(R.id.itemDescriptionTextView);
        TextView itemPriceTextView = convertView.findViewById(R.id.itemPriceTextView);
        TextView itemPostedByTextView = convertView.findViewById(R.id.itemPostedByTextView);
        TextView itemDateTextView = convertView.findViewById(R.id.itemDateTextView);

        if (item != null) {
            // Item name with status if not available
            if (!"available".equals(item.getStatus())) {
                itemNameTextView.setText(item.getName() + " [" + item.getStatus() + "]");
            } else {
                itemNameTextView.setText(item.getName());
            }

            // NEW: Show description
            if (item.getDescription() != null && !item.getDescription().trim().isEmpty()) {
                itemDescriptionTextView.setText(item.getDescription());
                itemDescriptionTextView.setVisibility(View.VISIBLE);
            } else {
                itemDescriptionTextView.setVisibility(View.GONE);
            }

            // Price or FREE
            if (item.isFree()) {
                itemPriceTextView.setText("FREE");
                itemPriceTextView.setTextColor(context.getResources()
                        .getColor(android.R.color.holo_green_dark));
            } else {
                itemPriceTextView.setText("$" + String.format("%.2f", item.getPrice()));
                itemPriceTextView.setTextColor(context.getResources()
                        .getColor(android.R.color.holo_blue_dark));
            }

            itemPostedByTextView.setText("Posted by: " + item.getPostedByName());

            // NEW: Better time formatting - shows relative time or date
            itemDateTextView.setText("Posted: " + formatPostedTime(item.getPostedAt()));
        }

        return convertView;
    }

    /**
     * Formats the posted timestamp into a human-readable string.
     * Shows relative time (e.g., "2 hours ago") for recent posts,
     * or full date for older posts.
     */
    /**
     * Formats the posted timestamp into a readable date and time string.
     */
    private String formatPostedTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.US);
        return sdf.format(new Date(timestamp));
    }
}