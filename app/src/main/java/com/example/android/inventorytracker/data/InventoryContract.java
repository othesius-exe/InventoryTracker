package com.example.android.inventorytracker.data;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * API Contract for the Inventory app
 */

public class InventoryContract {

    private InventoryContract() {}

    // Content Authority for the app.
    public static final String CONTENT_AUTHORITY = "com.example.android.inventorytracker";

    // Base Uri for queries
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    // Possible path for querying the Inventory table.
    public static final String PATH_INVENTORY = "inventory";

    /** Inner class that defines constants for the inventory database table
     * Each entry in the table represents a single inventory item
     */
    public static final class InventoryEntry implements BaseColumns {

        // The Content Uri to access Inventory data in the provider
        public static final Uri CONTENT_URI = Uri.withAppendedPath(BASE_CONTENT_URI, PATH_INVENTORY);

        // The MIME type the content Uri for a list of Inventory items
        public static final String CONTENT_LIST_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        // Mime type for a single Inventory item
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + CONTENT_AUTHORITY + "/" + PATH_INVENTORY;

        // Name of Database table
        public static final String TABLE_NAME = "inventory";

        // Unique ID for each item
        // Type: Integer
        public static final String _ID = BaseColumns._ID;

        // Item name
        // Type: Text
        public static final String COLUMN_ITEM_NAME = "name";

        // Item Description
        // Type: Integer
        public static final String COLUMN_ITEM_DESCRIPTION = "description";

        // Item price
        // Type: Integer
        public static final String COLUMN_ITEM_PRICE = "price";

        // Quantity in stock
        // Type: Integer
        public static final String COLUMN_ITEM_QUANITITY = "quantity";

        // Possible values for the description, listed in the spinner dropdown
        public static final int UNKNOWN = 0;
        public static final int ELECTRONIC = 1;
        public static final int ENTERTAINMENT = 2;
        public static final int HEALTH_BEAUTY = 3;
        public static final int HOUSEWARES = 4;
        public static final int CONSUMABLES = 5;
        public static final int CLOTHING = 6;

        public static boolean isValidDescription(int description) {
            if (description == UNKNOWN || description == ELECTRONIC || description == ENTERTAINMENT
                    || description == HEALTH_BEAUTY || description == HOUSEWARES
                    || description == CONSUMABLES || description == CLOTHING)
                return true;
            else {
                return false;
            }
        }
    }
}
