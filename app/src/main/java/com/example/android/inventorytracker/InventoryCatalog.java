package com.example.android.inventorytracker;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.inventorytracker.data.InventoryContract;
import com.example.android.inventorytracker.data.InventoryContract.InventoryEntry;

public class InventoryCatalog extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // Identifier for the loader
    private static final int INVENTORY_LOADER = 0;

    // Cursor Adapter
    InventoryCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory_catalog);

        // Setup the FAB to open the editor Activity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent detailIntent = new Intent(InventoryCatalog.this, DetailActivity.class);
                startActivity(detailIntent);
            }
        });

        // Find the ListView to populate with the Inventory List
        ListView itemListView = (ListView) findViewById(R.id.list);

        // Set an EmptyView on the List to display when the list is empty
        View emptyView = findViewById(R.id.empty_view);
        itemListView.setEmptyView(emptyView);

        // Setup a CursorAdapter to display the data in the cursor
        // Set to null since there is no data yet
        mCursorAdapter = new InventoryCursorAdapter(this, null);
        itemListView.setAdapter(mCursorAdapter);

        // Setup the itemClickListener to handle clicks on items in the list
        itemListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent detailIntent = new Intent(InventoryCatalog.this, DetailActivity.class);

                // Build the uri for the current item (Item that was clicked)
                Uri currentItemUri = ContentUris.withAppendedId(InventoryContract.InventoryEntry.CONTENT_URI, id);

                // Set the Uri on the data field of the intent
                detailIntent.setData(currentItemUri);

                // Launch the detail activity to display the information of the current item
                startActivity(detailIntent);
            }
        });

        // Start the loader
        getSupportLoaderManager().initLoader(INVENTORY_LOADER, null, this);
    }

    /**
     * Helper method for inserting test data
     */
    private void insertItem() {
        // Build a ContentValues object for an inventory item
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_ITEM_NAME, "Bottled Water");
        values.put(InventoryEntry.COLUMN_ITEM_DESCRIPTION, InventoryEntry.CONSUMABLES);
        values.put(InventoryEntry.COLUMN_ITEM_PRICE, 1.99);
        values.put(InventoryEntry.COLUMN_ITEM_QUANTITY, 10);

        // Uri for test data
        Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
    }

    /**
     * Method to delete all Items in the table
     */
    private void deleteAllItems() {
        int rowsDeleted = getContentResolver().delete(InventoryEntry.CONTENT_URI, null, null);
        Log.v("InventoryCatalog", rowsDeleted + " rows deleted from inventory database");
    }

    /**
     * Methods to activate Options Items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to click on Insert Test Data
            case R.id.action_insert_test_data:
                insertItem();
                return true;
            case R.id.action_delete_all:
                deleteAllItems();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the options menu from the res menu_catalog.xml file
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Define a projection that specifies the columns we want
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_ITEM_NAME,
                InventoryEntry.COLUMN_ITEM_DESCRIPTION,
                InventoryEntry.COLUMN_ITEM_PRICE,
                InventoryEntry.COLUMN_ITEM_QUANTITY };

        // Return the CursorLoader
        return new CursorLoader(this,       // Parent Activity
                InventoryEntry.CONTENT_URI, // Content Uri to query
                projection,                 // Projection
                null,                       // No selection clause
                null,                       // No selectionArgs
                null);                      // Default sortOrder
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Update the InventoryCursorAdapter with the new Cursor
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Called when data is to be deleted
        mCursorAdapter.swapCursor(null);
    }

}
