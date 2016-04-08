/*
 * Created by Stefan Sprenger
 */

package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;

import java.util.LinkedHashMap;
import java.util.Map;


public class StockDetailFragment extends Fragment {

    public static final String DETAIL_URI = "DETAIL_URI";

    private LineChartView mLineChartView;


    public StockDetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();

        Map<String, Float> chartMap = new LinkedHashMap<>();

        if (arguments != null) {

            Uri symbolUri = arguments.getParcelable(StockDetailFragment.DETAIL_URI);

            Cursor c = getContext().getContentResolver().query(symbolUri,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, QuoteColumns.ISCURRENT + "= ?",
                    new String[]{"0"}, QuoteColumns._ID + " DESC");

            System.out.println("HISTORY ENTRIES");
            DatabaseUtils.dumpCursor(c);

            int historyCount = 1;

            while (c.moveToNext() && historyCount < 10) {
                System.out.println("USED ENTRY: "+c.getString(c.getColumnIndex(QuoteColumns._ID)));
                System.out.println("USED SYMBOL: "+c.getString(c.getColumnIndex(QuoteColumns.SYMBOL)));
                float bidPrice = Float.parseFloat(c.getString(c.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",", "."));
                chartMap.put("-" + historyCount, bidPrice);
                historyCount++;
            }


            Cursor cursorActual = getContext().getContentResolver().query(symbolUri,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, QuoteColumns.ISCURRENT + "= ?",
                    new String[]{"1"}, null);

            cursorActual.moveToFirst();

            chartMap.put("0", Float.parseFloat(cursorActual.getString(cursorActual.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",", ".")));
        }

        View rootView = inflater.inflate(R.layout.stock_detail_fragment, container, false);
        mLineChartView = (LineChartView) rootView.findViewById(R.id.linechartstock);

        StringBuilder contentDescriptionChart = new StringBuilder();
        contentDescriptionChart.append("This chart shows the stock history for TODO.");

        float minVal = Float.MAX_VALUE;
        float maxVal = 0;

        LineSet dataset = new LineSet();

        // FIXME look which side
        for (Map.Entry<String, Float> historicEntry : chartMap.entrySet()) {

            contentDescriptionChart.append("Stock closing value on date " + historicEntry.getKey() + " is " + historicEntry.getValue() + ".");

            dataset.addPoint(historicEntry.getKey(), historicEntry.getValue());

            if (maxVal < historicEntry.getValue()) {
                maxVal = historicEntry.getValue();
            }

            if (minVal > historicEntry.getValue()) {
                minVal = historicEntry.getValue();
            }
        }
        dataset.setColor(Color.YELLOW);

        mLineChartView.dismiss();
        mLineChartView.addData(dataset);
        mLineChartView.setAxisBorderValues((int) minVal - 2, (int) maxVal + 2);
        mLineChartView.setAxisColor(Color.WHITE);
        mLineChartView.setLabelsColor(Color.WHITE);

        if(maxVal-minVal>0) {
            mLineChartView.setStep((int) Math.ceil(maxVal - minVal));
        }else{
            mLineChartView.setStep(1);
        }
        mLineChartView.setGrid(ChartView.GridType.FULL, new Paint(Color.WHITE));
        mLineChartView.show();


        mLineChartView.setContentDescription(contentDescriptionChart.toString());


        return rootView;
    }


}
