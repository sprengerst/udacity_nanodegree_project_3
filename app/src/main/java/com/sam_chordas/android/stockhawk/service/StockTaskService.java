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
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol "
                    + "in (", "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if (params.getTag().equals("init") || params.getTag().equals("periodic")) {
            isUpdate = true;
            initQueryCursor = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null,
                    null, null);

            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                if (!Utils.isConnected(mContext)) {
                    Utils.sendMessage("You have to run this app with internet for the first time", "critical", mContext);
                }
                // Init task. Populates DB with quotes for the symbols seen below
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
                    mStoredSymbols.append("\"").append(initQueryCursor.getString(initQueryCursor.getColumnIndex("symbol"))).append("\",");
                    initQueryCursor.moveToNext();
                }

                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");

                try {
                    urlStringBuilder.append(URLEncoder.encode(mStoredSymbols.toString(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals("add")) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            String stockInput = params.getExtras().getString("symbol");
            try {
                urlStringBuilder.append(URLEncoder.encode("\"" + stockInput + "\")", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys&callback=");

        String urlString;
        String getResponse;
        int result = GcmNetworkManager.RESULT_FAILURE;

        urlString = urlStringBuilder.toString();
        getResponse = fetchData(urlString);
        if (getResponse != null) {
            result = GcmNetworkManager.RESULT_SUCCESS;
            try {
                ContentValues contentValues = new ContentValues();
                // update ISCURRENT to 0 (false) so new data is current
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
            Utils.sendMessage("Problems while fetching/updating data, please check your internet connection", "normal", mContext);
        }


        return result;
    }

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
                        , new String[]{"0"}, QuoteColumns._ID + " DESC LIMIT 5");

                System.out.println("ENTRIES TO DELETE REFER");
                DatabaseUtils.dumpCursor(allSymbolEntriesCursor);

                if (allSymbolEntriesCursor != null && allSymbolEntriesCursor.getCount() != 0) {
                    allSymbolEntriesCursor.moveToLast();
                    System.out.println("SYMBOL DELETE " + symbolUri.getLastPathSegment());
                    System.out.println("DELETE ALL SMALLER THEN " + allSymbolEntriesCursor.getString(allSymbolEntriesCursor.getColumnIndex(QuoteColumns._ID)));
                    deleteCount += mContext.getContentResolver().delete(symbolUri,
                            QuoteColumns._ID + "< ?",
                            new String[]{allSymbolEntriesCursor.getString(allSymbolEntriesCursor.getColumnIndex(QuoteColumns._ID))});
                }
                allSymbolsCursor.moveToNext();

            }
        }

    }


    public static final String ACTION_DATA_UPDATED =
            "com.sam_chordas.android.stockhawk.ACTION_DATA_UPDATED";

    private void updateWidgets() {
        System.out.println("UPDATE WIDGET");
        // Setting the package ensures that only components in our app will receive the broadcast
        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED)
                .setPackage(mContext.getPackageName());
        mContext.sendBroadcast(dataUpdatedIntent);
    }


}
