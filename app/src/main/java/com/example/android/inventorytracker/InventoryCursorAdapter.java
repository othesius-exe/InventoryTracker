package com.example.android.inventorytracker;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Find the views in the list_item file that will house the information
        TextView nameView = (TextView) view.findViewById(R.id.item_name_view);
        final TextView quantityView = (TextView) view.findViewById(R.id.item_quantity_view);
        TextView priceView = (TextView) view.findViewById(R.id.item_price_view);
        Button decrement = (Button) view.findViewById(R.id.list_sale_button);

        // Get the columns of the item attributes to display
        int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
        int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANTITY);
        int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);

        // Read the attributes from the cursor
        String itemName = cursor.getString(nameColumnIndex);
        String itemQuantity = cursor.getString(quantityColumnIndex);
        String itemPrice = cursor.getString(priceColumnIndex);

        nameView.setText(itemName);
        quantityView.setText(itemQuantity);
        priceView.setText(itemPrice);

        final int rowID = cursor.getInt(cursor.getColumnIndex(InventoryEntry._ID));
        final int tempQuanitity = Integer.parseInt(itemQuantity);

        decrement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (tempQuanitity > 0) {
                    int newQuantity = tempQuanitity - 1;

                    ContentValues decrementValue = new ContentValues();
                    Uri updateUri = ContentUris.withAppendedId(InventoryEntry.CONTENT_URI, rowID);
                    decrementValue.put(InventoryEntry.COLUMN_ITEM_QUANTITY, newQuantity);
                    context.getContentResolver().update(updateUri,
                            decrementValue, null, null);

                    quantityView.setText(Integer.toString(newQuantity));
                }
            }
        });
    }
}
