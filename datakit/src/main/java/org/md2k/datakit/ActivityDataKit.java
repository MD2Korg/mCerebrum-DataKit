package org.md2k.datakit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.md2k.datakit.manager.FileManager;
import org.md2k.utilities.Apps;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.ActivityAbout;
import org.md2k.utilities.UI.ActivityCopyright;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
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

public class ActivityDataKit extends Activity {
    private static final String TAG = ActivityDataKit.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_kit);
        final Button buttonService = (Button) findViewById(R.id.buttonServiceStartStop);

        buttonService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityDataKit.this, ServiceDataKit.class);

                if (buttonService.getText().equals("Start Service")) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });
        findViewById(R.id.textViewTime).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityDataKit.this, ServiceDataKit.class);
                if (((TextView) findViewById(R.id.textViewTime)).getText().equals("OFF")) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
            }
        });
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

    }
    void updateSDCardSetttingsText(){
        ((TextView)findViewById(R.id.textview_sdcard_settings)).setText(FileManager.getCurrentSDCardOptionString());
        ((TextView)findViewById(R.id.textview_location_sd)).setText(FileManager.getValidSDcard(ActivityDataKit.this));
        ((TextView)findViewById(R.id.textview_location_db)).setText(FileManager.getFilePath(ActivityDataKit.this));
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(runnable);
        super.onPause();
    }
    @Override
    public void onResume() {
        updateSDCardSetttingsText();
        mHandler.post(runnable);

        super.onResume();
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
            case R.id.action_settings:
                intent = new Intent(this, ActivityDataKitSettings.class);
                startActivity(intent);
                break;
            case R.id.action_about:
                intent = new Intent(this, ActivityAbout.class);
                startActivity(intent);
                break;
            case R.id.action_copyright:
                intent = new Intent(this, ActivityCopyright.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    Handler mHandler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                long time = Apps.serviceRunningTime(ActivityDataKit.this, Constants.SERVICE_NAME);
                if (time < 0) {
                    ((TextView) findViewById(R.id.textViewTime)).setText("OFF");

                    ((Button) findViewById(R.id.buttonServiceStartStop)).setText("Start Service");
                    findViewById(R.id.buttonServiceStartStop).setBackground(getResources().getDrawable(R.drawable.button_green));

                } else {
                    long runtime = time / 1000;
                    int second = (int) (runtime % 60);
                    runtime /= 60;
                    int minute = (int) (runtime % 60);
                    runtime /= 60;
                    int hour = (int) runtime;
                    ((TextView) findViewById(R.id.textViewTime)).setText(String.format("%02d:%02d:%02d", hour, minute, second));
                    ((Button) findViewById(R.id.buttonServiceStartStop)).setText("Stop Service");
                    findViewById(R.id.buttonServiceStartStop).setBackground(getResources().getDrawable(R.drawable.button_red));

                }
                mHandler.postDelayed(this, 1000);
            }
        }
    };

}
