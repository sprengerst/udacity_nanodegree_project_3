
/*
 * Created by Stefan Sprenger
 */

package com.sam_chordas.android.stockhawk.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.sam_chordas.android.stockhawk.R;

public class StockDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.stock_detail_activity);
        if (savedInstanceState == null) {

            Bundle bundle = getIntent().getExtras();
            float todayVal = bundle.getFloat("TODAYVAL");
            String chosenTag = bundle.getString("TAG");

            Bundle arguments = new Bundle();
            arguments.putString("TAG", chosenTag);
            arguments.putFloat("TODAYVAL", todayVal);

            StockDetailFragment fragment = new StockDetailFragment();
            fragment.setArguments(arguments);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.movie_detail_container, fragment)
                    .commit();
        }
    }


}