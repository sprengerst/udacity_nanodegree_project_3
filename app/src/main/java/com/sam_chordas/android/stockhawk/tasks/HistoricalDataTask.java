package com.sam_chordas.android.stockhawk.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;


public class HistoricalDataTask extends AsyncTask<String, Void, Map<String, Float>> {

    private final Context mContext;
    private final String LOG_TAG = HistoricalDataTask.class.getSimpleName();
    private OkHttpClient client = new OkHttpClient();

    public HistoricalDataTask(Context context) {
        this.mContext = context;
    }

    @Override
    protected Map<String, Float> doInBackground(String... strings) {
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String stockTag = strings[0];

        StringBuilder urlStringBuilder = new StringBuilder();

        try {

            Date date = new Date();

            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = "
                    + " \"" + stockTag + "\" and startDate = \"" + Utils.addDays(date, -7) + "\" and endDate = \"" + Utils.getDateString(date) + "\"", "UTF-8"));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String urlString;
        String getResponse;

        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");
        if (urlStringBuilder != null) {
            urlString = urlStringBuilder.toString();
            try {
                getResponse = fetchData(urlString);
                System.out.println("GETRESPONSE: " + getResponse);
                return getHistoricDataFromJSON(getResponse);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;

    }

    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        Response response = client.newCall(request).execute();
        return response.body().string();
    }


    private Map<String,Float> getHistoricDataFromJSON(String historicChartDataJSON)
            throws JSONException, ParseException {

        final String CONTAINER = "query";

        final String HISTORIC_LIST = "results";
        final String HISTORIC_LIST_QUOTE = "quote";

        final String CLOSEVALUE = "Close";
        final String CLOSEDATE = "Date";

        JSONObject pageJSON = new JSONObject(historicChartDataJSON);
        JSONObject queryContainer = pageJSON.getJSONObject(CONTAINER);
        JSONObject listHolder = queryContainer.getJSONObject(HISTORIC_LIST);
        JSONArray resultArray = listHolder.getJSONArray(HISTORIC_LIST_QUOTE);

        Map<String,Float> resultMap = new TreeMap<>();

        for (int i = 0; i < resultArray.length(); i++) {

            JSONObject singleMovieJSON = resultArray.getJSONObject(i);

            String closeValue = singleMovieJSON.getString(CLOSEVALUE);
            String closeDate = singleMovieJSON.getString(CLOSEDATE);

            System.out.println("CLOSEVALUE: "+closeValue);
            System.out.println("CLOSEDAATE: "+closeDate);

            resultMap.put( Utils.chartFormat(closeDate),Float.parseFloat(closeValue));
        }


        return resultMap;
    }


}



