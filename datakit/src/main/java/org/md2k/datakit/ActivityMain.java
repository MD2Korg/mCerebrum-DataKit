package org.md2k.datakit;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.core.CrashlyticsCore;

import org.md2k.datakit.cerebralcortex.ActivityCerebralCortexSettings;
import org.md2k.datakit.cerebralcortex.CerebralCortexController;
import org.md2k.datakit.cerebralcortex.ServiceCerebralCortex;
import org.md2k.datakit.operation.FileManager;
import org.md2k.datakit.privacy.PrivacyController;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Apps;
import org.md2k.utilities.UI.ActivityAbout;
import org.md2k.utilities.UI.ActivityCopyright;

import java.io.IOException;
import java.lang.reflect.Method;

import io.fabric.sdk.android.Fabric;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
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

public class ActivityMain extends AppCompatActivity {
    private static final String TAG = ActivityMain.class.getSimpleName();
    PrivacyController privacyController;

    CerebralCortexController cerebralCortexController;

    Handler mHandler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                long time = Apps.serviceRunningTime(getApplicationContext(), Constants.SERVICE_NAME);
                if (time < 0) {
                    ((Button) findViewById(R.id.button_app_status)).setText(R.string.inactive);
                    findViewById(R.id.button_app_status).setBackground(ContextCompat.getDrawable(ActivityMain.this, R.drawable.button_status_off));
                } else {
                    long runtime = time / 1000;
                    int second = (int) (runtime % 60);
                    runtime /= 60;
                    int minute = (int) (runtime % 60);
                    runtime /= 60;
                    int hour = (int) runtime;
                    ((Button) findViewById(R.id.button_app_status)).setText(String.format("%02d:%02d:%02d", hour, minute, second));
                }
                updateUI();
                updateCCUI();
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            Fabric.with(this, new Crashlytics.Builder().core(new CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build()).build());
            setContentView(R.layout.activity_main);
            configureAppStatus();
            setupPrivacyUI();
            setupCerebralCortexUI();
            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void configureAppStatus() {
        findViewById(R.id.textViewTap).setVisibility(View.GONE);
    }


    void setupCerebralCortexUI() throws IOException {
        cerebralCortexController = CerebralCortexController.getInstance(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ll_cerebralcortex);

        if (!cerebralCortexController.isAvailable()) {
            linearLayout.setVisibility(View.GONE);
        } else {

            linearLayout.setVisibility(View.VISIBLE);
            updateCCUI();

            Button button = (Button) findViewById(R.id.button_cerebralcortex);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ActivityMain.this, ServiceCerebralCortex.class);
                    if (!cerebralCortexController.isActive()) {
                        startService(intent);
                    } else {
                        stopService(intent);
                    }

                }
            });
        }
    }


    void setupPrivacyUI() throws IOException {
        privacyController = new PrivacyController(this);
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.ll_privacy);
        if (!privacyController.isAvailable()) {
            linearLayout.setVisibility(View.GONE);
        } else {
            linearLayout.setVisibility(View.VISIBLE);
            updateUI();

            Button button = (Button) findViewById(R.id.button_privacy);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(ActivityMain.this, ActivityPrivacy.class);
                    startActivity(intent);
                }
            });
        }
    }

    public void updateCCUI() {
        TextView textViewStatus = ((TextView) findViewById(R.id.textViewcerebralcortexStatus));
        if (cerebralCortexController.isActive()) {
            textViewStatus.setText("ON (Running)");
            textViewStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
        } else {
            textViewStatus.setText("OFF");
            textViewStatus.setTextColor(ContextCompat.getColor(this, R.color.red_700));
        }
    }

    public void updateUI(){
        if (!privacyController.isAvailable()) return;
        TextView textViewStatus=((TextView) findViewById(R.id.textViewPrivacyStatus));
        TextView textViewOption=((TextView) findViewById(R.id.textViewPrivacyOption));

        if (privacyController.isActive()) {
            textViewStatus.setText("ON ("+ DateTime.convertTimestampToTimeStr(privacyController.getRemainingTime()) + ")");
            textViewOption.setText(privacyController.getActiveList());
            textViewStatus.setTextColor(ContextCompat.getColor(this,R.color.red_700));
            textViewOption.setTextColor(ContextCompat.getColor(this, R.color.red_200));

        } else {
            textViewStatus.setText("OFF");
            textViewOption.setText("");
            textViewStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700));
            textViewOption.setVisibility(View.GONE);
            findViewById(R.id.textViewPrivacyOptionTitle).setVisibility(View.GONE);

        }

    }

    void updateSDCardSetttingsText() {
        ((TextView) findViewById(R.id.textview_sdcard_settings)).setText(FileManager.getCurrentSDCardOptionString());
        ((TextView) findViewById(R.id.textview_location_sd)).setText(FileManager.getValidSDcard(ActivityMain.this));
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
    protected boolean onPrepareOptionsPanel(View view, Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {

                }
            }
        }
        return super.onPrepareOptionsPanel(view, menu);
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

            case R.id.action_cerebralcortex_settings:
                intent = new Intent(this, ActivityCerebralCortexSettings.class);
                startActivity(intent);
                break;

            case R.id.action_about:
                intent = new Intent(this, ActivityAbout.class);
                try {
                    intent.putExtra(org.md2k.utilities.Constants.VERSION_CODE, String.valueOf(this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode));
                    intent.putExtra(org.md2k.utilities.Constants.VERSION_NAME, this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                startActivity(intent);
                break;
            case R.id.action_copyright:
                intent = new Intent(this, ActivityCopyright.class);
                startActivity(intent);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


}
