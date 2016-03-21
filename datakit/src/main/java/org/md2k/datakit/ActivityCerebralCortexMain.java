package org.md2k.datakit;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;

import org.md2k.datakit.cerebralcortex.ServiceCerebralCortex;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Apps;

import java.util.HashMap;

import io.fabric.sdk.android.Fabric;

/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * - Timothy Hnat <twhnat@memphis.edu>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

public class ActivityCerebralCortexMain extends AppCompatActivity {
    private static final String TAG = ActivityCerebralCortexMain.class.getSimpleName();
    static HashMap<Integer, Long> currentData = new HashMap<>();
    static HashMap<Integer, String> timeData = new HashMap<>();
    Handler mHandler = new Handler();
    HashMap<String, TextView> hashMapData = new HashMap<>();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                long time = Apps.serviceRunningTime(ActivityCerebralCortexMain.this, Constants.CC_SERVICE_NAME);
                if (time < 0) {
                    ((Button) findViewById(R.id.button_app_status)).setText("START");
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityCerebralCortexMain.this, R.drawable.button_status_off));

                } else {
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityCerebralCortexMain.this, R.drawable.button_status_on));
                    ((Button) findViewById(R.id.button_app_status)).setText(DateTime.convertTimestampToTimeStr(time));

                }
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
//            updateTable(intent);
        }
    };

//    private void updateTable(Intent intent) {
//        try {
//            int streamID = intent.getIntExtra("streamID", -1);
//            long dataIndex = intent.getLongExtra("index", -1);
//
//            if (!currentData.containsKey(streamID)) {
//                currentData.put(streamID, dataIndex);
//            }
//            if (!timeData.containsKey(streamID)) {
//                if (dataIndex > 0)
//                    timeData.put(streamID, new Timestamp(System.currentTimeMillis()).toString());
//            }
//            if (!hashMapData.containsKey(streamID + "_index")) {
//                prepareTable(streamID + 1);
//                for (Map.Entry<Integer, Long> entry : currentData.entrySet()) {
//                    hashMapData.get(entry.getKey() + "_index").setText(entry.getValue().toString());
//                }
//                for (Map.Entry<Integer, String> entry : timeData.entrySet()) {
//                    if (dataIndex > 0)
//                        hashMapData.get(entry.getKey() + "_update").setText(entry.getValue());
//                }
//
//            }
//
//            hashMapData.get(streamID + "_index").setText(Long.toString(dataIndex));
//            if (dataIndex > 0)
//                hashMapData.get(streamID + "_update").setText(new Timestamp(System.currentTimeMillis()).toString());
//        } catch (NullPointerException e) {
//            Log.d("CerebralCortex", "Null pointer in update table");
//            Crashlytics.logException(e);
//        }
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        final Button buttonService = (Button) findViewById(R.id.button_app_status);

        buttonService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ServiceCerebralCortex.class);
                if (Apps.isServiceRunning(getBaseContext(), Constants.CC_SERVICE_NAME)) {
                    stopService(intent);
                } else {
                    startService(intent);
                }
            }
        });
        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent intent;
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                break;
            case R.id.action_cerebralcortex_settings:
                intent = new Intent(this, ActivityCerebralCortexSettings.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("cerebralCortex"));
        prepareTable(currentData.size());
        mHandler.post(runnable);
        super.onResume();
    }

//    TableRow createDefaultRow() {
//        TableRow row = new TableRow(this);
//        TextView tvSensor = new TextView(this);
//        tvSensor.setText("Stream");
//        tvSensor.setTypeface(null, Typeface.BOLD);
//        tvSensor.setTextColor(getResources().getColor(R.color.teal_700));
//        TextView tvindex = new TextView(this);
//        tvindex.setText("Last Table Index");
//        tvindex.setTypeface(null, Typeface.BOLD);
//        tvindex.setTextColor(getResources().getColor(R.color.teal_700));
//
//        TextView tvUpdate = new TextView(this);
//        tvUpdate.setText("Last Update");
//        tvUpdate.setTypeface(null, Typeface.BOLD);
//        tvUpdate.setTextColor(getResources().getColor(R.color.teal_700));
//
//        row.addView(tvSensor);
//        row.addView(tvindex);
//        row.addView(tvUpdate);
//        return row;
//    }

    private void prepareTable(int streams) {
//        TableLayout ll = (TableLayout) findViewById(R.id.tableLayout);
//        ll.removeAllViews();
//        ll.addView(createDefaultRow());
//        for (int i = 1; i <= streams; i++) {
//            TableRow row = new TableRow(this);
//            TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT);
//            row.setLayoutParams(lp);
//            TextView tvStream = new TextView(this);
//            tvStream.setText("" + i);
//
//            TextView tvIndex = new TextView(this);
//
//            if (currentData.containsKey(i)) {
//                tvIndex.setText(currentData.get(i).toString());
//            } else {
//                tvIndex.setText("");
//            }
//            hashMapData.put(i + "_index", tvIndex);
//
//            TextView tvUpdate = new TextView(this);
//            if (timeData.containsKey(i)) {
//                tvUpdate.setText(timeData.get(i).toString());
//            } else {
//                tvUpdate.setText("");
//            }
//            hashMapData.put(i + "_update", tvUpdate);
//
//            row.addView(tvStream);
//            row.addView(tvIndex);
//            row.addView(tvUpdate);
//            ll.addView(row);
//        }
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(runnable);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
