package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.RecyclerViewItemClickListener;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;

public class StockOverviewActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private CharSequence mTitle;
    private Intent mServiceIntent;
    private static final int CURSOR_LOADER_ID = 0;
    private QuoteCursorAdapter mCursorAdapter;
    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;

        setContentView(R.layout.stock_overview_activity);

        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {
            initService();
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        assert recyclerView != null;

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        if (Utils.isConnected(mContext)) {
                            Intent intent = new Intent(mContext, StockDetailActivity.class).setData(QuoteProvider.Quotes.withSymbol(((TextView) (v.findViewById(R.id.stock_symbol))).getText().toString()));
                            startActivity(intent);
                        } else {
                            Utils.showToast(getString(R.string.err_stock_detail_network), mContext);
                        }
                    }
                }));

        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
                                   @Override
                                   public void onClick(View v) {

                                       if (Utils.isConnected(mContext)) {
                                           new MaterialDialog.Builder(mContext).title(R.string.symbol_search)
                                                   .content(R.string.content_test)
                                                   .inputType(InputType.TYPE_CLASS_TEXT)
                                                   .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                                       @Override
                                                       public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                                           if (0 != input.length()) {
                                                               Cursor c = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                                                       new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                                                       new String[]{input.toString().toUpperCase()}, null);
                                                               assert c != null;
                                                               if (c.getCount() != 0) {
                                                                   Toast toast =
                                                                           Toast.makeText(StockOverviewActivity.this, R.string.err_stock_duplicate,
                                                                                   Toast.LENGTH_LONG);
                                                                   toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                                                   toast.show();
                                                                   return;
                                                               } else {
                                                                   mServiceIntent.putExtra(getString(R.string.service_state_tag), getString(R.string.service_state_add));
                                                                   mServiceIntent.putExtra(getString(R.string.service_symbol), input.toString().toUpperCase());
                                                                   startService(mServiceIntent);
                                                               }

                                                               c.close();
                                                           } else {
                                                               Utils.showToast(getString(R.string.err_add_empty_symbol), dialog.getContext());
                                                           }

                                                       }
                                                   })
                                                   .show();
                                       } else {
                                           Utils.showToast(getString(R.string.err_add_only_if_network), mContext);
                                       }
                                   }
                               }
        );

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);

        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();

//        long period = 3600L;
        long period = 3L; // TODO
        long flex = 10L;

        PeriodicTask periodicTask = new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(getString(R.string.service_state_periodic))
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();

        GcmNetworkManager.getInstance(this).schedule(periodicTask);
    }

    private void initService() {
        mServiceIntent.putExtra(getString(R.string.service_state_tag), getString(R.string.service_state_init));
        startService(mServiceIntent);
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter(getString(R.string.broadcast_resource_stock)));
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stock_activity_settings, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_change_units) {
            Utils.showPercent = !Utils.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }


    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                mMessageReceiver);
        super.onPause();
    }

    private final BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(getString(R.string.priority_message));
            String state = intent.getStringExtra(getString(R.string.priority_state));

            if (state.equals(getString(R.string.priority_alert_critical))) {
                forceDataWithMessage(message);
            } else {
                Utils.showToast(message, context);
            }
        }
    };

    public void forceDataWithMessage(final String message) {
        if (!Utils.isConnected(this)) {
            try {
                AlertDialog.Builder builder =
                        new AlertDialog.Builder(this, R.style.InternetAlertDialogStyle);
                builder.setTitle(getResources().getString(R.string.app_name));
                builder.setMessage(message);
                builder.setPositiveButton(getString(R.string.force_data_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        forceDataWithMessage(message);
                    }
                });
                builder.setCancelable(false);
                builder.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            initService();
        }
    }

}
