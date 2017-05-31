package com.example.android.inventorytracker.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.android.inventorytracker.data.InventoryContract.InventoryEntry;

/**
 * Database Helper for Inventory App. Handles database creation and version management.
 */

public class InventoryDbHelper extends SQLiteOpenHelper {

    /** Log tag */
    public static final String LOG_TAG = InventoryDbHelper.class.getSimpleName();

    // Name of the database file
    private static final String DATABASE_NAME = "inventory.db";

    // Database version number
    private static final int DATABASE_VERSION = 1;

    /**
     * InventoryDbHelper Constructor
     */
    public InventoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Called when the database is created the first time
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // String used for creating a database
        String SQL_CREATE_INVENTORY_TABLE = "CREATE TABLE " + InventoryEntry.TABLE_NAME + " ("
                + InventoryEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + InventoryEntry.COLUMN_ITEM_NAME + " TEXT NOT NULL, "
                + InventoryEntry.COLUMN_ITEM_DESCRIPTION + " INTEGER NOT NULL, "
                + InventoryEntry.COLUMN_ITEM_PRICE + " REAL NOT NULL, "
                //+ InventoryEntry.COLUMN_ITEM_IMAGE + " BLOB, "
                + InventoryEntry.COLUMN_ITEM_QUANTITY + " INTEGER NOT NULL DEFAULT 0);";

        // Execute SQL
        db.execSQL(SQL_CREATE_INVENTORY_TABLE);
    }

    /**
     * Called when the table needs to be upgraded
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
