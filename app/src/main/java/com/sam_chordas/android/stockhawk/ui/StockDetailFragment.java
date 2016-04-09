/*
 * Created by Stefan Sprenger
 */

package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
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
            assert symbolUri != null;
            String symbol = symbolUri.getLastPathSegment();
            Map<String, Float> chartMap = new LinkedHashMap<>();

            Cursor chartEntriesCursor = getContext().getContentResolver().query(symbolUri,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP,
                            QuoteColumns.CREATEDATE},   QuoteColumns.ISCURRENT + "= ?"+ ") GROUP BY (createdate",
                    new String[]{"0"}, QuoteColumns._ID + " ASC");

            // fill chartMap with cursor entries
            if(chartEntriesCursor != null) {
                while (chartEntriesCursor.moveToNext()) {
                    float bidPrice = Float.parseFloat(chartEntriesCursor.getString(chartEntriesCursor.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",", "."));
                    String createdDate = chartEntriesCursor.getString(chartEntriesCursor.getColumnIndex(QuoteColumns.CREATEDATE));
                    chartMap.put(createdDate, bidPrice);
                }

                chartEntriesCursor.close();
            }

            // add today entry
            Cursor cursorToday = getContext().getContentResolver().query(symbolUri,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, QuoteColumns.ISCURRENT + "= ?",
                    new String[]{"1"}, null);

            if(cursorToday!= null && cursorToday.getCount()!=0) {
                cursorToday.moveToFirst();
                chartMap.put(getActivity().getString(R.string.chart_actual_text), Float.parseFloat(cursorToday.getString(cursorToday.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",", ".")));
                cursorToday.close();
            }

            // fill chart with data
            StringBuilder contentDescriptionChart = new StringBuilder();
            contentDescriptionChart.append(getActivity().getString(R.string.accessibility_chart_intro)).append(symbol);

            float minVal = Float.MAX_VALUE;
            float maxVal = 0;

            List<Entry> data = new ArrayList<>();
            List<String> labels = new ArrayList<>();

            int i = 0;
            for (Map.Entry<String, Float> historicEntry : chartMap.entrySet()) {

                contentDescriptionChart.append(getActivity().getString(R.string.accessibility_chart_value)).append(historicEntry.getKey()).append(" = ").append(historicEntry.getValue()).append(".");

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

            LineDataSet lineDataSet = new LineDataSet(data, getActivity().getString(R.string.chart_points_text));

            lineDataSet.setDrawCubic(true);
            lineDataSet.setDrawFilled(true);

            LineData combinedLineData = new LineData(labels, lineDataSet);

            combinedLineData.setValueTextColor(Color.WHITE);

            LineChart mLineChartView = (LineChart) rootView.findViewById(R.id.linechartsstock);

            XAxis xAxis = mLineChartView.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setTextSize(4f);
            xAxis.setTextColor(Color.WHITE);
            xAxis.setDrawAxisLine(true);
            xAxis.setDrawGridLines(true);

            mLineChartView.getAxisLeft().setTextColor(Color.WHITE);
            mLineChartView.getAxisRight().setTextColor(Color.WHITE);
            mLineChartView.setData(combinedLineData);
            mLineChartView.setDescription(getActivity().getString(R.string.chart_description) + symbol);
            mLineChartView.setDescriptionColor(Color.WHITE);
            mLineChartView.setBorderColor(Color.WHITE);

            mLineChartView.setContentDescription(contentDescriptionChart.toString());

        }

        return rootView;
    }


}
