/*
 * Created by Stefan Sprenger
 */

package com.sam_chordas.android.stockhawk.ui;

import android.database.Cursor;
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
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.tasks.HistoricalDataTask;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;


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
        String tag = null;
        float todayVal = 0;
        if (arguments != null) {
            Uri uri = arguments.getParcelable(StockDetailFragment.DETAIL_URI);
            String symbol = uri.getPathSegments().get(1);

            Cursor c = getContext().getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                    new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                            QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP}, QuoteColumns.SYMBOL + "= ?",
                    new String[]{symbol.toUpperCase()}, null);

            c.moveToFirst();

            tag = c.getString(c.getColumnIndex(QuoteColumns.SYMBOL));
            todayVal = Float.parseFloat(c.getString(c.getColumnIndex(QuoteColumns.BIDPRICE)).replace(",","."));

        }

        View rootView = inflater.inflate(R.layout.stock_detail_fragment, container, false);
        mLineChartView = (LineChartView) rootView.findViewById(R.id.linechartstock);

        StringBuilder contentDescriptionChart = new StringBuilder();
        contentDescriptionChart.append("This chart shows the stock history for " + tag + ".");

        try {

            Map<String, Float> chartMap = new HistoricalDataTask(getActivity()).execute(new String[]{tag}).get();

            System.out.println("TODAYVAL :" + todayVal);

            if (!chartMap.containsKey(Utils.chartFormat(Utils.getDateString(new Date()))
            )) {
                chartMap.put(Utils.chartFormat(Utils.getDateString(new Date())), todayVal);
            }

            float minVal = Float.MAX_VALUE;
            float maxVal = 0;

            LineSet dataset = new LineSet();
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
            mLineChartView.setStep((int) Math.ceil(maxVal - minVal));
            mLineChartView.setGrid(ChartView.GridType.FULL, new Paint(Color.WHITE));
            mLineChartView.show();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        mLineChartView.setContentDescription(contentDescriptionChart.toString());


        return rootView;
    }


}
