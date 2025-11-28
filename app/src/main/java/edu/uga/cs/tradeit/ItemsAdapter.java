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
import edu.uga.cs.tradeit.Item;

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
        TextView itemPriceTextView = convertView.findViewById(R.id.itemPriceTextView);
        TextView itemPostedByTextView = convertView.findViewById(R.id.itemPostedByTextView);
        TextView itemDateTextView = convertView.findViewById(R.id.itemDateTextView);

        if (item != null) {
            itemNameTextView.setText(item.getName());

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

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.US);
            String dateStr = sdf.format(new Date(item.getPostedAt()));
            itemDateTextView.setText(dateStr);

            // Add status indicator if not available
            if (!"available".equals(item.getStatus())) {
                itemNameTextView.setText(item.getName() + " [" + item.getStatus() + "]");
            }
        }

        return convertView;
    }
}
