package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 *
 * Modified by Stefan Sprenger
 */
public class StockTaskService extends GcmTaskService {
    private String LOG_TAG = StockTaskService.class.getSimpleName();

    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService() {
        super();
    }

    public StockTaskService(Context context) {
        mContext = context;
    }

    String fetchData(String url) {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try {
            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public int onRunTask(TaskParams params) {
        Cursor initQueryCursor;
        if (mContext == null) {
            mContext = this;
        }
        StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append(getString(R.string.yahoo_api_https));
            urlStringBuilder.append(URLEncoder.encode(getString(R.string.yahoo_api_select_quotes), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (params.getTag().equals(getString(R.string.service_state_init)) || params.getTag().equals(getString(R.string.service_state_periodic))) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);

            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                if (!Utils.isConnected(mContext)) {
                    Utils.sendMessage(getString(R.string.err_run_app_with_internet_ft), getString(R.string.priority_alert_critical), mContext);
                }

                try {
                    urlStringBuilder.append(
                            URLEncoder.encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"").append(initQueryCursor.getString(initQueryCursor.getColumnIndex(QuoteColumns.SYMBOL))).append("\",");
                    initQueryCursor.moveToNext();
                }

                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");

                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals(getString(R.string.service_state_add))) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString(getString(R.string.service_symbol));
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append(getString(R.string.yahoo_api_additional_callback));

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();
        getResponse = fetchData(urlString);
        if (getResponse != null) {
            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                ContentValues contentValues = new ContentValues();
                if (isUpdate) {
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    mContext.getContentResolver().update(QuoteProvider.Quotes.CONTENT_URI, contentValues,
                            null, null);
                }
                mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY, Utils.quoteJsonToContentVals(getResponse, mContext));
                clearHistoryAllSymbols();

            } catch (RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            }

            updateWidgets();

        } else {
            Utils.sendMessage(getString(R.string.err_fetching_updating_data), getString(R.string.priority_alert_normal), mContext);
        }

        return result;
    }

    // deletes the history for all symbols except for amount of specified in the chat_history_limit_count string
    private void clearHistoryAllSymbols() {
        Cursor allSymbolsCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                null, null);

        int deleteCount = 0;

        if (allSymbolsCursor != null && allSymbolsCursor.getCount() != 0) {
            allSymbolsCursor.moveToFirst();

            for (int i = 0; i < allSymbolsCursor.getCount(); i++) {

                Uri symbolUri = QuoteProvider.Quotes.withSymbol(allSymbolsCursor.getString(allSymbolsCursor.getColumnIndex(QuoteColumns.SYMBOL)));
                Cursor allSymbolEntriesCursor = mContext.getContentResolver().query(symbolUri,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, QuoteColumns.ISCURRENT + "= ?"
                        , new String[]{"0"}, QuoteColumns._ID + " DESC LIMIT "+getString(R.string.chart_history_limit_count));

                if (allSymbolEntriesCursor != null && allSymbolEntriesCursor.getCount() != 0) {
                    allSymbolEntriesCursor.moveToLast();
                    deleteCount += mContext.getContentResolver().delete(symbolUri,
                            QuoteColumns._ID + "< ?",
                            new String[]{allSymbolEntriesCursor.getString(allSymbolEntriesCursor.getColumnIndex(QuoteColumns._ID))});
                }
                allSymbolsCursor.moveToNext();

            }
            allSymbolsCursor.close();
        }
    }

    public static final String ACTION_DATA_UPDATED =
            "com.sam_chordas.android.stockhawk.ACTION_DATA_UPDATED";

    private void updateWidgets() {
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(dataUpdatedIntent);
    }
}
