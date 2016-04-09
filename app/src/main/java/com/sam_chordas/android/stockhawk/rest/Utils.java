package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by sam_chordas on 10/8/15.
 * <p/>
 * <p/>
 * Modified by Stefan Sprenger
 */
public class Utils {

    private static final String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String JSON, Context context) {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        JSONObject jsonObject;
        JSONArray resultsArray;
//        System.out.println("JSON : " + JSON.replaceAll("\",", "\n\","));
        try {
            jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(context.getString(R.string.json_yahoo_query));
                int count = Integer.parseInt(jsonObject.getString(context.getString(R.string.json_yahoo_count)));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(context.getString(R.string.json_yahoo_results))
                            .getJSONObject(context.getString(R.string.json_yahoo_quote));

                    if (validJSONObject(jsonObject, context)) {
                        batchOperations.add(buildBatchOperation(jsonObject, context));
                    } else {
                        sendMessage(context.getString(R.string.non_valid_stock_symbol_add), context.getString(R.string.priority_alert_normal), context);
                    }
                } else {
                    resultsArray = jsonObject.getJSONObject(context.getString(R.string.json_yahoo_results)).getJSONArray(context.getString(R.string.json_yahoo_quote));

                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            jsonObject = resultsArray.getJSONObject(i);

                            if (validJSONObject(jsonObject, context)) {
                                batchOperations.add(buildBatchOperation(jsonObject, context));
                            } else {
                                sendMessage(context.getString(R.string.non_valid_stock_symbol_add), context.getString(R.string.priority_alert_normal), context);
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }

        return batchOperations;
    }

    public static void sendMessage(String errorMessage, String state, Context context) {
        Intent intent = new Intent(context.getString(R.string.broadcast_resource_stock));
        intent.putExtra(context.getString(R.string.priority_message), errorMessage);
        intent.putExtra(context.getString(R.string.priority_state), state);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    private static boolean validJSONObject(JSONObject jsonObject, Context context) throws JSONException {
        return !(context.getString(R.string.json_yahoo_nullstring)).equals(jsonObject.getString(context.getString(R.string.json_yahoo_bid))) &&
                !(context.getString(R.string.json_yahoo_nullstring)).equals(jsonObject.getString(context.getString(R.string.json_yahoo_changeinpercent)));
    }

    private static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    private static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuilder changeBuffer = new StringBuilder(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }

    private static ContentProviderOperation buildBatchOperation(JSONObject jsonObject, Context context) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString(context.getString(R.string.json_yahoo_change));
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(context.getString(R.string.json_yahoo_symbol)));

            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString(context.getString(R.string.json_yahoo_bid))));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString(context.getString(R.string.json_yahoo_changeinpercent)), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.CREATEDATE, getCurrentDateFormat(context));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    private static String getCurrentDateFormat(Context context) {
        SimpleDateFormat outputYahooFormat = new SimpleDateFormat(context.getString(R.string.chart_date_format));
        return outputYahooFormat.format(new Date());
    }

    public static void showToast(String toastText, Context context) {
        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show();
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

}
