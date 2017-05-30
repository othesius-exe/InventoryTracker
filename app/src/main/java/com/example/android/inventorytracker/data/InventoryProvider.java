package com.example.android.inventorytracker.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import com.example.android.inventorytracker.data.InventoryContract.InventoryEntry;

/**
 * {@Link ContentProvider} for Inventory app
 */

public class InventoryProvider extends ContentProvider {

    /** Log Tag */
    public static final String LOG_TAG = InventoryProvider.class.getSimpleName();

    /** URI Matcher Code for Inventory table */
    private static final int INVENTORY = 100;

    /** URI Matcher Code for Singe Item in Inventory */
    private static final int INVENTORY_ID = 101;

    /** URI Mather Object Constructor matches Content URI to Corresponding Code
     * The input passed into the constructor represents the code to return for the root URI
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // Static Initializer; Run the first time anything is called from this class.
    static {

        // URI Matcher for entire table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY, INVENTORY);

        // URI Matcher for single item in a table.
        sUriMatcher.addURI(InventoryContract.CONTENT_AUTHORITY, InventoryContract.PATH_INVENTORY + "/#", INVENTORY_ID);
    }

    /** Database Helper Object */
    private InventoryDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new InventoryDbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        // Get readable database
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // Cursor object to hold the query result
        Cursor cursor;

        // Try to match the URI to a specific code
        int match = sUriMatcher.match(uri);

        switch (match) {
            case INVENTORY:
                // In case of INVENTORY code, query the table with the given projection
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            case INVENTORY_ID:
                // In case of INVENTORY_ID code, query the table for the given id.
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // Return a cursor containing the row with the given id.
                cursor = database.query(InventoryEntry.TABLE_NAME, projection, selection,
                        selectionArgs, null, null, sortOrder);
                break;
            default: throw new IllegalArgumentException("Cannot query unknown Uri" + uri);
        }

        // Set the notification uri
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        // Return the cursor
        return cursor;
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return insertItem(uri, contentValues);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }
    }

    /**
     * Insert an item into the Inventory table
     * Return the content Uri for the row
     */
    private Uri insertItem(Uri uri, ContentValues contentValues) {

        // Check that name is not null
        String name = contentValues.getAsString(InventoryEntry.COLUMN_ITEM_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Item requires a name.");
        }

        // Check that description is selected
        Integer description = contentValues.getAsInteger(InventoryEntry.COLUMN_ITEM_DESCRIPTION);
        if (description == null || !InventoryEntry.isValidDescription(description)) {
            throw new IllegalArgumentException("Description must be selected");
        }

        // Check for a price
         Float price = contentValues.getAsFloat(InventoryEntry.COLUMN_ITEM_PRICE);
            if (price == null) {
                throw new IllegalArgumentException("Price must be included");
            }

        // Check that a quantity has been included
        Integer quantity = contentValues.getAsInteger(InventoryEntry.COLUMN_ITEM_QUANITITY);
        if (quantity != null && quantity < 0) {
            throw new IllegalArgumentException("Quanity requires a valid number");
        }

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new item with the given values
        long id = database.insert(InventoryEntry.TABLE_NAME, null, contentValues);

        // If the id is -1 then insertion failed
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        // Notify all listeners that the
        getContext().getContentResolver().notifyChange(uri, null);

        // Return the new Uri with the id of the newly created row appended to the end
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return updateItem(uri, contentValues, selection, selectionArgs);
            case INVENTORY_ID:
                // Extract the row ID and update the correct row
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri)) };
                return updateItem(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update not supported for " + uri);
        }
    }

    /**
     * Update items in the Inventory table
     * Return number of rows updated
     */
    private int updateItem(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {

        // Check for a name
        if (contentValues.containsKey(InventoryEntry.COLUMN_ITEM_NAME)) {
            String name = contentValues.getAsString(InventoryEntry.COLUMN_ITEM_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Item requires a name");
            }
        }

        // Check for a description
        if (contentValues.containsKey(InventoryEntry.COLUMN_ITEM_DESCRIPTION)) {
            Integer description = contentValues.getAsInteger(InventoryEntry.COLUMN_ITEM_DESCRIPTION);
            if (description == null || !InventoryEntry.isValidDescription(description)) {
                throw new IllegalArgumentException("Description must be selected");
            }
        }

        // Check for a price
        if (contentValues.containsKey(InventoryEntry.COLUMN_ITEM_PRICE)) {
            Float price = contentValues.getAsFloat(InventoryEntry.COLUMN_ITEM_PRICE);
            if (price == null) {
                throw new IllegalArgumentException("Price must be included");
            }
        }

        // Check for a quantity
        if (contentValues.containsKey(InventoryEntry.COLUMN_ITEM_QUANITITY)) {
            Integer quantity = contentValues.getAsInteger(InventoryEntry.COLUMN_ITEM_QUANITITY);
            if (quantity != null && quantity < 0) {
                throw new IllegalArgumentException("Item requires valid quantity");
            }
        }

        // If there are no values present, then do not update the item
        if (contentValues.size() == 0) {
            return 0;
        }

        // Otherwise write new values to the database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Store the number of rows updated
        int rowsUpdated = database.update(InventoryEntry.TABLE_NAME, contentValues, selection, selectionArgs);

        // Notify listeners of any changes made
        if (rowsUpdated != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return the number or rows updated
        return rowsUpdated;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        // Get writable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Store number of rows deleted
        int rowsDeleted;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                // Delete all rows that match the selection and selectionArgs
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case INVENTORY_ID:
                selection = InventoryEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                rowsDeleted = database.delete(InventoryEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }

        // If rows were deleted, notify listeners of the change
        if (rowsDeleted != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }

        // Return rows deleted
        return rowsDeleted;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case INVENTORY:
                return InventoryEntry.CONTENT_LIST_TYPE;
            case INVENTORY_ID:
                return InventoryEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }
}