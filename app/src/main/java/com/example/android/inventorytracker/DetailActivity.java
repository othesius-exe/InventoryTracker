// Icon Images Courtesy of Icons8
// Camera implementation uses some code from https://github.com/crlsndrsjmnz/MyFileProviderExample

package com.example.android.inventorytracker;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
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
import android.widget.Toast;

import com.example.android.inventorytracker.data.InventoryContract.InventoryEntry;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Activity to handle displaying details of individual items in the inventory.
 */

public class DetailActivity extends AppCompatActivity
        implements android.app.LoaderManager.LoaderCallbacks<Cursor> {

    private static final int EXISTING_ITEM_LOADER = 0;

    public Uri mCurrentItemUri;

    private EditText mItemEditText;
    private EditText mInStockEditText;
    private EditText mPriceEditText;
    private EditText mBuySellEditText;

    Button mReceiveShipmentButton;
    Button mBuyStockButton;
    Button mSellStockButton;
    Button mCameraButton;

    private Spinner mDescriptionSpinner;

    private int mDescription = InventoryEntry.UNKNOWN;

    private boolean mItemHasChanged = false;

    private ImageView mItemImage;

    private static final String LOG_TAG = DetailActivity.class.getSimpleName();

    private static final int PICK_IMAGE_REQUEST = 0;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int MY_PERMISSIONS_REQUEST = 2;
    private boolean isGalleryPicture = false;

    private static final String FILE_PROVIDER_AUTHORITY = "com.example.android.fileprovider";


    private Uri mUri;
    private Bitmap mBitmap;
    public String mInvoiceSummary;

    private static final String JPEG_FILE_PREFIX = "IMG_";
    private static final String JPEG_FILE_SUFFIX = ".jpg";

    private static final String CAMERA_DIR = "/dcim/";

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

        requestPermissions();

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
                takePicture();
            }
        });

        mBuyStockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String orderSummary = mItemEditText.getText().toString()
                        + "\n" + getString(R.string.num_to_change) + " "
                        +  mBuySellEditText.getText().toString();

                Intent placeOrder = new Intent(Intent.ACTION_SENDTO);
                placeOrder.setData(Uri.parse("mailto:"));
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

                String forSale = mBuySellEditText.getText().toString();
                String inStock = mInStockEditText.getText().toString();
                String salePrice = mPriceEditText.getText().toString();

                int numToSell = 0;
                int stockInt = 0;
                Double itemPrice = 0.00;

                try {
                    numToSell = Integer.parseInt(forSale);
                    stockInt = Integer.parseInt(inStock);
                    itemPrice = Double.parseDouble(salePrice);
                    Log.i("", numToSell + " is a number");
                    Log.i("", stockInt + " is a number");
                } catch (NumberFormatException e) {
                    Log.i("", numToSell + " is not a number");
                    Log.i("", stockInt + " is not a number");
                }
                Double totalSale = 0.00;
                if (numToSell > 0 && stockInt > 0 && stockInt - numToSell >= 0) {
                    int newStock = stockInt - numToSell;
                    totalSale = itemPrice * numToSell;
                    mInStockEditText.setText(Integer.toString(newStock));
                    StringBuilder invoiceString = new StringBuilder();
                    invoiceString.append(mItemEditText.getText().toString());
                    invoiceString.append("\n");
                    invoiceString.append(mPriceEditText.getText().toString());
                    invoiceString.append("\n\n" + getResources().getString(R.string.num_to_change) + " ");
                    invoiceString.append(mBuySellEditText.getText().toString());
                    invoiceString.append("\n\n" + getResources().getString(R.string.sale));
                    invoiceString.append("\n");
                    invoiceString.append(totalSale);

                    mInvoiceSummary = invoiceString.toString();

                    Intent sendInvoice = new Intent(Intent.ACTION_SENDTO);
                    sendInvoice.setData(Uri.parse("mailto:"));
                    sendInvoice.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.invoice));
                    sendInvoice.putExtra(Intent.EXTRA_TEXT, mInvoiceSummary);

                    if (sendInvoice.resolveActivity(getPackageManager()) != null) {
                        startActivity(sendInvoice);

                    }
                } else {
                    Toast.makeText(DetailActivity.this, "Cannot sell negative number of items", Toast.LENGTH_SHORT).show();

                }

            }
        });

        mReceiveShipmentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                receiveStock();
            }
        });
    }

    /**
     * Helper method for incrementing the amount in stock
     */
    public void receiveStock() {
        String currentStock = mInStockEditText.getText().toString();
        String addStock = mBuySellEditText.getText().toString();
        int currentStockInt = Integer.parseInt(currentStock);
        int addStockInt = Integer.parseInt(addStock);
        int newStockTotalInt = currentStockInt + addStockInt;
        String newStockTotalString = Integer.toString(newStockTotalInt);
        mInStockEditText.setText(newStockTotalString);
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
        String unitPrice = mPriceEditText.getText().toString().trim();
        String photoPath = "";

        if (mUri != null) {
            photoPath = mUri.getPath();
            mItemImage.setTag(photoPath);
        }

        // Check to see if this is a new item
        if (mCurrentItemUri == null && TextUtils.isEmpty(name) || TextUtils.isEmpty(inStock)
                || mDescription == InventoryEntry.UNKNOWN) {
            Toast.makeText(this, "All Fields Must Be Filled Out", Toast.LENGTH_LONG).show();
            return;
        }

        // Build a ContentValues with the input
        ContentValues values = new ContentValues();
        values.put(InventoryEntry.COLUMN_ITEM_NAME, name);
        values.put(InventoryEntry.COLUMN_ITEM_DESCRIPTION, mDescription);
        values.put(InventoryEntry.COLUMN_ITEM_IMAGE, photoPath);

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
                InventoryEntry.COLUMN_ITEM_IMAGE,
                InventoryEntry.COLUMN_ITEM_PRICE};

        return new CursorLoader(this,   // Parent activity context
                mCurrentItemUri,         // Query the content URI for the current item
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
            // Find the columns of item attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_NAME);
            int descColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_DESCRIPTION);
            int priceColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_PRICE);
            int imageColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_IMAGE);
            int quantityColumnIndex = cursor.getColumnIndex(InventoryEntry.COLUMN_ITEM_QUANTITY);

            // Extract out the value from the Cursor for the given column index
            String name = cursor.getString(nameColumnIndex);
            int description = cursor.getInt(descColumnIndex);
            float price = cursor.getInt(priceColumnIndex);
            int quantity = cursor.getInt(quantityColumnIndex);
            String imageString = cursor.getString(imageColumnIndex);
            Uri imageUri = Uri.parse("content://" + FILE_PROVIDER_AUTHORITY + imageString);

            // Update the views on the screen with the values from the database
            mItemEditText.setText(name);
            mPriceEditText.setText(String.format("%.2f", price));
            mInStockEditText.setText(Integer.toString(quantity));
            mItemImage.setImageBitmap(getBitmapFromUri(imageUri));



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

    public void requestPermissions() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA},
                        MY_PERMISSIONS_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && grantResults[1] == PackageManager.PERMISSION_GRANTED
                        && grantResults[2] == PackageManager.PERMISSION_GRANTED) {


                    mCameraButton.setEnabled(true);
                }
            }
        }
    }

    public void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        try {
            File f = createImageFile();

            Log.d(LOG_TAG, "File: " + f.getAbsolutePath());

            mUri = FileProvider.getUriForFile(
                    this, FILE_PROVIDER_AUTHORITY, f);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mUri);

            // Solution taken from http://stackoverflow.com/a/18332000/3346625
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                List<ResolveInfo> resInfoList = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, mUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        Log.i(LOG_TAG, "Received an \"Activity Result\"");
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE.
        // If the request code seen here doesn't match, it's the response to some other intent,
        // and the below code shouldn't run at all.

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.  Pull that uri using "resultData.getData()"

            if (resultData != null) {
                mUri = resultData.getData();
                Log.i(LOG_TAG, "Uri: " + mUri.toString());

                mBitmap = getBitmapFromUri(mUri);
                mItemImage.setImageBitmap(mBitmap);

                isGalleryPicture = true;
            }
        } else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            Log.i(LOG_TAG, "Uri: " + mUri.toString());

            mBitmap = getBitmapFromUri(mUri);
            mItemImage.setImageBitmap(mBitmap);

            isGalleryPicture = false;
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) {
        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to load image.", e);
            return null;
        } finally {
            try {
                if (parcelFileDescriptor != null) {
                    parcelFileDescriptor.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, "Error closing ParcelFile Descriptor");
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = JPEG_FILE_PREFIX + timeStamp + "_";
        File albumF = getAlbumDir();
        File imageF = File.createTempFile(imageFileName, JPEG_FILE_SUFFIX, albumF);
        return imageF;
    }

    private File getAlbumDir() {
        File storageDir = null;

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            storageDir = new File(Environment.getExternalStorageDirectory()
                    + CAMERA_DIR
                    + getString(R.string.app_name));

            Log.d(LOG_TAG, "Dir: " + storageDir);

            if (storageDir != null) {
                if (!storageDir.mkdirs()) {
                    if (!storageDir.exists()) {
                        Log.d(LOG_TAG, "failed to create directory");
                        return null;
                    }
                }
            }

        } else {
            Log.v(getString(R.string.app_name), "External storage is not mounted READ/WRITE.");
        }

        return storageDir;
    }
}