/*
 * Created by Stefan Sprenger
 */

package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class StockDetailFragment extends Fragment {

    public static final String DETAIL_URI = "DETAIL_URI";


    public StockDetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();

        View rootView = inflater.inflate(R.layout.stock_detail_fragment, container, false);

        if (arguments != null) {
            Uri symbolUri = arguments.getParcelable(StockDetailFragment.DETAIL_URI);
            String symbol = symbolUri.getLastPathSegment();
            Map<String, Float> chartMap = new LinkedHashMap<>();

            Cursor c = getContext().getContentResolver().query(symbolUri,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP, QuoteColumns.CREATEDATE}, QuoteColumns.ISCURRENT + "= ?",
                    new String[]{"0"}, QuoteColumns._ID + " ASC");

            DatabaseUtils.dumpCursor(c);


            while (c.moveToNext()) {
                System.out.println("USED ENTRY: " + c.getString(c.getColumnIndex(QuoteColumns._ID)));
                System.out.println("USED SYMBOL: " + c.getString(c.getColumnIndex(QuoteColumns.SYMBOL)));
                float bidPrice = Float.parseFloat(c.getString(c.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",", "."));
                String createdDate = c.getString(c.getColumnIndex(QuoteColumns.CREATEDATE));
                System.out.println("CREATEDATE:" + createdDate);
                chartMap.put(createdDate, bidPrice);
            }


            Cursor cursorActual = getContext().getContentResolver().query(symbolUri,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, QuoteColumns.ISCURRENT + "= ?",
                    new String[]{"1"}, null);

            cursorActual.moveToFirst();

            chartMap.put("Actual", Float.parseFloat(cursorActual.getString(cursorActual.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",", ".")));

            LineChart mLineChartView = (LineChart) rootView.findViewById(R.id.linechartsstock);

            StringBuilder contentDescriptionChart = new StringBuilder();
            contentDescriptionChart.append("This chart shows the stock history for TODO.");

            float minVal = Float.MAX_VALUE;
            float maxVal = 0;


            List<Entry> data = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            int i = 0;
            for (Map.Entry<String, Float> historicEntry : chartMap.entrySet()) {

                contentDescriptionChart.append("Stock closing value on date " + historicEntry.getKey() + " is " + historicEntry.getValue() + ".");

                data.add(new Entry(historicEntry.getValue(), i));
                labels.add(historicEntry.getKey());

                if (maxVal < historicEntry.getValue()) {
                    maxVal = historicEntry.getValue();
                }

                if (minVal > historicEntry.getValue()) {
                    minVal = historicEntry.getValue();
                }
                i++;
            }

            System.out.println("DATA LENGTH: " + data.size());

            LineDataSet dataset = new LineDataSet(data, "Bid Price");

            dataset.setDrawCubic(true);
            dataset.setDrawFilled(true);

            LineData combinedLineData = new LineData(labels, dataset);

            combinedLineData.setValueTextColor(Color.WHITE);

            XAxis xAxis = mLineChartView.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(4f);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(true);

            mLineChartView.getAxisLeft().setTextColor(Color.WHITE);
            mLineChartView.getAxisRight().setTextColor(Color.WHITE);
            mLineChartView.setData(combinedLineData);
            mLineChartView.setDescription("Overview for Stock: " + symbol);
            mLineChartView.setDescriptionColor(Color.WHITE);
            mLineChartView.setBorderColor(Color.WHITE);

            mLineChartView.setContentDescription(contentDescriptionChart.toString());



        }

        return rootView;
    }


}
