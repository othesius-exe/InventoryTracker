package com.example.android.inventorytracker;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventorytracker.data.InventoryContract.InventoryEntry;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Activity to handle displaying details of individual items in the inventory.
 */

public class DetailActivity extends AppCompatActivity
        implements android.app.LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_ITEM_LOADER = 0;

    private Uri mCurrentItemUri;

    private EditText mItemEditText;

    private EditText mInStockEditText;

    private EditText mPriceEditText;

    private EditText mBuySellEditText;

    private Button mReceiveShipmentButton;

    private Button mBuyStockButton;

    private Button mSellStockButton;

    private Button mCameraButton;

    private Spinner mDescriptionSpinner;

    private int mDescription = InventoryEntry.UNKNOWN;

    private boolean mItemHasChanged = false;

    private ImageView mItemImage;

    static final int REQUEST_TAKE_PHOTO = 1;

    private String mCurrentPhotoPath;

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            mItemHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        // Check for an existing Uri to determine if this is a new item
        Intent intent = getIntent();
        mCurrentItemUri = intent.getData();

        // If there is no Uri then this is a new item
        // Set the activity title as Create Item Listing
        if (mCurrentItemUri == null) {
            setTitle(R.string.create_new_item);
            // Invalidate the options menu to hide the delete function
            invalidateOptionsMenu();
        } else {
            setTitle(R.string.edit_item);

            getLoaderManager().initLoader(EXISTING_ITEM_LOADER, null, this);
        }

        // Edit Text for inputting item name
        mItemEditText = (EditText) findViewById(R.id.item_name_edit_text);
        // Edit Text to display current amount in stock
        mInStockEditText = (EditText) findViewById(R.id.in_stock_edit_text);
        // Edit Text to buy/sell stock
        mBuySellEditText = (EditText) findViewById(R.id.buy_sell_edit_text);
        // Button will increase stock by amount in the above text edit
        mBuyStockButton = (Button) findViewById(R.id.place_order_button);
        // Button will decrease stock by the amount in mBuySellEditText
        mSellStockButton = (Button) findViewById(R.id.make_sale_button);
        // Button to receive Shipment
        mReceiveShipmentButton = (Button) findViewById(R.id.receive_shipment_button);
        // Spinner with possible item categories
        mDescriptionSpinner = (Spinner) findViewById(R.id.item_description_spinner);
        // Price edit text
        mPriceEditText = (EditText) findViewById(R.id.price_edit_text);
        // ImageView
        mItemImage = (ImageView) findViewById(R.id.image_view);
        // Camera Button
        mCameraButton = (Button) findViewById(R.id.camera_button);

        mItemEditText.setOnTouchListener(mTouchListener);
        mPriceEditText.setOnTouchListener(mTouchListener);
        mBuySellEditText.setOnTouchListener(mTouchListener);
        mDescriptionSpinner.setOnTouchListener(mTouchListener);

        setupSpinner();

        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        mBuyStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String orderSummary = mItemEditText.getText().toString()
                        + mBuySellEditText.getText().toString();

                Intent placeOrder = new Intent(Intent.ACTION_SENDTO);
                placeOrder.setData(Uri.parse("mailto:"));
                placeOrder.putExtra(Intent.EXTRA_EMAIL, getString(R.string.supplier_email));
                placeOrder.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.product_order));
                placeOrder.putExtra(Intent.EXTRA_TEXT, orderSummary);

                if (placeOrder.resolveActivity(getPackageManager()) != null) {
                    startActivity(placeOrder);
                }
            }
        });


        mSellStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sellStock();
            }
        });
    }

    /**
     * Helper Method for making a sale
     */
    public void sellStock() {
        String saleInput = mBuySellEditText.getText().toString();
        String currentStock = mInStockEditText.getText().toString();
        String currentPrice = mPriceEditText.getText().toString();
        Double price = Double.parseDouble(currentPrice);
        int numToSell = 0;
        int stockInt = 0;

        Dialog saleDialog = new Dialog(this);
        saleDialog.setContentView(R.layout.popupview);
        saleDialog.setTitle(getString(R.string.sale_title));
        TextView priceView = (TextView) findViewById(R.id.sale_price);



        try {
            numToSell = Integer.parseInt(saleInput);
            stockInt = Integer.parseInt(currentStock);
            Log.i("", numToSell + " is a number");
            Log.i("", stockInt + " is a number");
        } catch (NumberFormatException e) {
            Log.i("", numToSell + " is not a number");
            Log.i("", stockInt + " is not a number");
        }

        int afterSaleStock;
        Double totalSalePrice;
        String afterSaleStockString;
        if (numToSell > 0 && stockInt - numToSell >= 0) {
            afterSaleStock = stockInt - numToSell;
            afterSaleStockString = Integer.toString(afterSaleStock);
            mInStockEditText.setText(afterSaleStockString);

            totalSalePrice = price * numToSell;
            String totalSalePriceString = Double.toString(totalSalePrice);
            priceView.setText(totalSalePriceString);
            saleDialog.show();
        } else {
            priceView.setText(getString(R.string.sale_failed));
            saleDialog.show();
        }
    }

    /**
     * Setup the spinner
     */
    private void setupSpinner() {
        ArrayAdapter descriptionSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.item_descriptors_array, android.R.layout.simple_spinner_item);
        descriptionSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        mDescriptionSpinner.setAdapter(descriptionSpinnerAdapter);

        mDescriptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = (String) parent.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.electronics))) {
                        mDescription = InventoryEntry.ELECTRONIC;
                    } else if (selection.equals(getString(R.string.clothing))) {
                        mDescription = InventoryEntry.CLOTHING;
                    } else if (selection.equals(getString(R.string.consumable))) {
                        mDescription = InventoryEntry.CONSUMABLES;
                    } else if (selection.equals(getString(R.string.health_beauty))) {
                        mDescription = InventoryEntry.HEALTH_BEAUTY;
                    } else if (selection.equals(getString(R.string.entertainment))) {
                        mDescription = InventoryEntry.ENTERTAINMENT;
                    } else if (selection.equals(getString(R.string.housewares))) {
                        mDescription = InventoryEntry.HOUSEWARES;
                    } else {
                        mDescription = InventoryEntry.UNKNOWN;
                    }

                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                mDescription = InventoryEntry.UNKNOWN;
            }
        });
    }

    /**
     * Get user input and save as item in database
     */
    private void saveItem() {
        String name = mItemEditText.getText().toString().trim();
        String inStock = mInStockEditText.getText().toString().trim();
        String sellStock = mBuySellEditText.getText().toString().trim();
        String unitPrice = mPriceEditText.getText().toString().trim();
        Bitmap itemImage = mItemImage.getDrawingCache();

        // Check to see if this is a new item
        if (mCurrentItemUri == null && TextUtils.isEmpty(name) && TextUtils.isEmpty(inStock)
                && TextUtils.isEmpty(sellStock) && mDescription == InventoryEntry.UNKNOWN) {
            return;
        }

        // Build a ContentValues with the input
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_ITEM_NAME, name);
        values.put(InventoryEntry.COLUMN_ITEM_DESCRIPTION, mDescription);

        // If a price is not included, set to 0
        Double price = 0.00;
        if (!TextUtils.isEmpty(unitPrice)) {
            price = Double.parseDouble(unitPrice);
        }
        values.put(InventoryEntry.COLUMN_ITEM_PRICE, price);

        // If a quantity is not provided, set to 0
        int stock = 0;
        if (!TextUtils.isEmpty(inStock)) {
            stock = Integer.parseInt(inStock);
        }
        values.put(InventoryEntry.COLUMN_ITEM_QUANTITY, stock);

        // TODO Check for an image


        // Determine if this is a new or existing item
        if (mCurrentItemUri == null) {
            Uri newUri = getContentResolver().insert(InventoryEntry.CONTENT_URI, values);

            if (newUri == null) {
                Toast.makeText(this, R.string.error_making_item, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.item_saved, Toast.LENGTH_SHORT).show();
            }
        } else {
            int rowsChanged = getContentResolver().update(mCurrentItemUri, values, null, null);

            if (rowsChanged == 0) {
                Toast.makeText(this, R.string.update_error, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.update_successful, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate an options menu from the res menu_detail.xml file
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mCurrentItemUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveItem();
                finish();
                return true;
            case R.id.action_delete:
                showDeleteConfirmationDialogue();
                return true;
            case R.id.home:
                if (!mItemHasChanged) {
                    NavUtils.navigateUpFromSameTask(DetailActivity.this);
                    return true;
                }
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(DetailActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mItemHasChanged) {
            super.onBackPressed();
            return;
        }

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        String[] projection = {
                InventoryEntry._ID,
                InventoryEntry.COLUMN_ITEM_NAME,
                InventoryEntry.COLUMN_ITEM_DESCRIPTION,
                InventoryEntry.COLUMN_ITEM_QUANTITY,
                InventoryEntry.COLUMN_ITEM_PRICE };

        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current pet
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of pet attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
            int descColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_DESCRIPTION);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANTITY);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int description = cursor.getInt(descColumnIndex);
            float price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);

            // Update the views on the screen with the values from the database
            mItemEditText.setText(name);
            mPriceEditText.setText(String.format("%.2f", price));
            mInStockEditText.setText(Integer.toString(quantity));

            // Description is a dropdown spinner, so map the constant value from the database
            // Then call setSelection() so that option is displayed on screen as the current selection.
            switch (description) {
                case InventoryEntry.ELECTRONIC:
                    mDescriptionSpinner.setSelection(1);
                    break;
                case InventoryEntry.ENTERTAINMENT:
                    mDescriptionSpinner.setSelection(2);
                    break;
                case InventoryEntry.HEALTH_BEAUTY:
                    mDescriptionSpinner.setSelection(3);
                    break;
                case InventoryEntry.HOUSEWARES:
                    mDescriptionSpinner.setSelection(4);
                    break;
                case InventoryEntry.CONSUMABLES:
                    mDescriptionSpinner.setSelection(5);
                    break;
                case InventoryEntry.CLOTHING:
                    mDescriptionSpinner.setSelection(6);
                default:
                    mDescriptionSpinner.setSelection(0);
                    break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        mItemEditText.setText("");
        mPriceEditText.setText("");
        mInStockEditText.setText("");
        mDescriptionSpinner.setSelection(0); // Select "Unknown"
    }

    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void showDeleteConfirmationDialogue() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deleteItem();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void deleteItem() {
        // Only perform the delete if this is an existing pet.
        if (mCurrentItemUri != null) {
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the pet that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentItemUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.deletion_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.deletion_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
}
