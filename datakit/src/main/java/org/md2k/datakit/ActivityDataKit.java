package org.md2k.datakit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import org.md2k.datakit.manager.FileManager;
import org.md2k.utilities.Apps;
import org.md2k.utilities.Report.Log;

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
        Switch service = (Switch) findViewById(R.id.switchService);
        service.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d(TAG, "isChecked=" + isChecked);
                if (isChecked) {
                    Intent intent = new Intent(ActivityDataKit.this, ServiceDataKit.class);
                    startService(intent);
                } else {
                    if (Apps.isServiceRunning(ActivityDataKit.this, Constants.SERVICE_NAME) == true) {
                        Intent intent = new Intent(ActivityDataKit.this, ServiceDataKit.class);
                        stopService(intent);
                    }
                }
            }
        });
    }
    void updateSDCardSetttingsText(){
        ((TextView)findViewById(R.id.textview_sdcard_settings)).setText(FileManager.getCurrentSDCardOptionString());
        ((TextView)findViewById(R.id.textview_location_sd)).setText(FileManager.getValidSDcard(ActivityDataKit.this));
        ((TextView)findViewById(R.id.textview_location_db)).setText(FileManager.getFilePath(ActivityDataKit.this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    void updateServiceSwitch(){
        Switch service = (Switch) findViewById(R.id.switchService);
        if (Apps.isServiceRunning(ActivityDataKit.this, Constants.SERVICE_NAME)) {
            service.setChecked(true);
        }
        else service.setChecked(false);
    }

    @Override
    public void onResume() {
        updateSDCardSetttingsText();
        updateServiceSwitch();
        super.onResume();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
//            Intent intent = new Intent(this, ActivityPhoneSensorSettings.class);
//            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
