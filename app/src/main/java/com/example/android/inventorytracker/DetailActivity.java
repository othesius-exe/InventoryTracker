package com.example.android.inventorytracker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;

/**
 * Activity to handle displaying details of individual items in the inventory.
 */

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate an options menu from the res menu_detail.xml file
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }
}
