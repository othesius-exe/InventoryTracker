package com.example.android.inventorytracker;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;

import com.example.android.inventorytracker.data.InventoryContract.InventoryEntry;
/**
 * Java Code to implement a custom cursor adapter to handle displaying new cursor objects in a List.
 */

public class InventoryCursorAdapter extends CursorAdapter {

    // CursorAdapter Constructor
    public InventoryCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    // Create the new view, without adding any data
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
    }

    // Binds the information to the view
    public void bindView(View view, Context context, Cursor cursor) {
        // Find the views in the list_item file that will house the information
        TextView nameView = (TextView) view.findViewById(R.id.item_name_view);
        TextView descriptView = (TextView) view.findViewById(R.id.item_description_view);

        // Get the columns of the item attributes to display
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
        int descColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_DESCRIPTION);

        // Read the attributes from the cursor
        String itemName = cursor.getString(nameColumnIndex);
        String itemDesc = cursor.getString(descColumnIndex);

        nameView.setText(itemName);
        descriptView.setText(itemDesc);

    }
}
