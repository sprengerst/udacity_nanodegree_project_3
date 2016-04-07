package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;

/**
 * Created by sam_chordas on 10/1/15.
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
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();
        if (intent.getStringExtra("tag").equals("add")) {
            args.putString("symbol", intent.getStringExtra("symbol"));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        try {
            stockTaskService.onRunTask(new TaskParams(intent.getStringExtra("tag"), args));
        } catch (NumberFormatException e) {
            sendMessage("You tried to enter a non-valid Stock Symbol");
        }
    }

    private void sendMessage(String errorMessage) {
        Intent intent = new Intent(getString(R.string.broadcast_resource_stock));
        intent.putExtra("message", errorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
