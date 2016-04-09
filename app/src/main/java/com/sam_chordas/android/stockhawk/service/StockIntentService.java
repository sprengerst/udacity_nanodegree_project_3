package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import com.sam_chordas.android.stockhawk.R;

import com.google.android.gms.gcm.TaskParams;

/**
 * Created by sam_chordas on 10/1/15.
 *
 * Modified by Stefan Sprenger
 */
public class StockIntentService extends IntentService {

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();

        if (intent.getStringExtra(getString(R.string.service_state_tag)).equals(getString(R.string.service_state_add))) {
            args.putString(getString(R.string.service_symbol), intent.getStringExtra(getString(R.string.service_symbol)));
        }

        stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(getBaseContext().getResources().getString(R.string.service_state_tag)), args));
    }
}
