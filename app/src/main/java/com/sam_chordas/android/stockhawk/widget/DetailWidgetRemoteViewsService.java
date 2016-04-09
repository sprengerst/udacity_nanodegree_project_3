package com.sam_chordas.android.stockhawk.widget;

import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

/**
 * RemoteViewsService controlling the data being shown in the scrollable weather detail widget
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class DetailWidgetRemoteViewsService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {
                // Nothing to do
            }

            @Override
            public void onDataSetChanged() {
                if (data != null) {
                    data.close();
                }

                final long identityToken = Binder.clearCallingIdentity();

                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);

                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION ||
                        data == null || !data.moveToPosition(position)) {
                    return null;
                }
                RemoteViews views = new RemoteViews(getPackageName(),
                        R.layout.widget_detail_list_item);

                String symbol = data.getString(data.getColumnIndex("symbol"));
                String bidPrice = data.getString(data.getColumnIndex("bid_price"));
                String percentChange = data.getString(data.getColumnIndex("percent_change"));

                views.setTextViewText(R.id.stock_symbol_widget, symbol);
                views.setTextViewText(R.id.bid_price_widget, bidPrice);


                if (data.getInt(data.getColumnIndex("is_up")) == 1) {
                        views.setInt(R.id.change_widget, "setBackgroundResource", R.drawable.percent_change_pill_green);
                } else {
                    views.setInt(R.id.change_widget, "setBackgroundResource", R.drawable.percent_change_pill_red);
                }
//
//                if (Utils.showPercent) {
//                    viewHolder.change.setContentDescription("Percentage Change" +cursor.getString(cursor.getColumnIndex("percent_change")));
//                    viewHolder.change.setText(cursor.getString(cursor.getColumnIndex("percent_change")));
//                } else {
//                    viewHolder.change.setContentDescription("Change "+cursor.getString(cursor.getColumnIndex("change")));
//                    viewHolder.change.setText(cursor.getString(cursor.getColumnIndex("change")));
//                }
//

                views.setTextViewText(R.id.change_widget, percentChange);

                final Intent fillInIntent = new Intent();

                Uri quoteUri = QuoteProvider.Quotes.withSymbol(symbol);
                fillInIntent.setData(quoteUri);

                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }


            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.layout.widget_detail_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }


            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(data.getColumnIndex(QuoteColumns._ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
