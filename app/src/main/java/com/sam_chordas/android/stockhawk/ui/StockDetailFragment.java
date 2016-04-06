/*
 * Created by Stefan Sprenger
 */

package com.sam_chordas.android.stockhawk.ui;

import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.db.chart.model.LineSet;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.tasks.HistoricalDataTask;

import java.util.Map;
import java.util.concurrent.ExecutionException;


public class StockDetailFragment extends Fragment {

    static final String DETAIL_URI = "DETAIL_URI";

    private LineChartView mLineChartView;


    public StockDetailFragment() {
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        String tag = null;
        if (arguments != null) {
            tag = arguments.getString("TAG");
        }

        View rootView = inflater.inflate(R.layout.stock_detail_fragment, container, false);
        mLineChartView = (LineChartView) rootView.findViewById(R.id.linechartstock);


        try {
            Map<String, Float> chartMap = new HistoricalDataTask(getActivity()).execute(new String[]{tag}).get();


            float minVal = Float.MAX_VALUE;
            float maxVal = 0;

            LineSet dataset = new LineSet();
            for (Map.Entry<String, Float> historicEntry : chartMap.entrySet()) {
                dataset.addPoint(historicEntry.getKey(), historicEntry.getValue());

                if(maxVal<historicEntry.getValue()){
                    maxVal = historicEntry.getValue();
                }

                if(minVal>historicEntry.getValue()){
                    minVal = historicEntry.getValue();
                }

                System.out.println("GETKEY" + historicEntry.getKey() + " VALUE: " + historicEntry.getValue());
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
        }


        return rootView;
    }


}
