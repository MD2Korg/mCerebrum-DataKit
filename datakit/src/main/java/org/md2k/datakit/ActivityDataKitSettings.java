package org.md2k.datakit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.view.MenuItem;
import android.widget.Toast;

import org.md2k.datakit.logger2.DatabaseLogger;
import org.md2k.datakit.manager.FileManager;
import org.md2k.utilities.Apps;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * <p/>
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * <p/>
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * <p/>
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

public class ActivityDataKitSettings extends PreferenceActivity {
    private static final String TAG = ActivityDataKitSettings.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datakit_settings);
        addPreferencesFromResource(R.xml.pref_datakit_general);
        setupPreferences();
        if (getActionBar() != null)
            getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    void setupPreferences() {
        setupDatabaseFile();
        setupDatabaseLocation();
        setupDatabaseClear();
        setupSDCardSpace();
        setupDatabaseSize();
    }
    void setupDatabaseLocation(){
        Preference preference = findPreference("database_location");
        preference.setSummary(FileManager.getValidSDcard(ActivityDataKitSettings.this));
    }
    void setupSDCardSpace(){
        Preference preference = findPreference("storage_space");
        preference.setSummary(FileManager.getStorageSpace(ActivityDataKitSettings.this));
    }
    void setupDatabaseSize(){
        Preference preference = findPreference("database_size");
        preference.setSummary(FileManager.getFileSize(ActivityDataKitSettings.this));
    }

    void setupDatabaseFile(){
        Preference preference = findPreference("database_filename");
        preference.setSummary(FileManager.getFilePath(ActivityDataKitSettings.this));

    }
    void setupDatabaseClear(){
        Preference preference = findPreference("database_clear");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog alertDialog = new AlertDialog.Builder(ActivityDataKitSettings.this).create();
                alertDialog.setTitle("Delete All Data");
                alertDialog.setMessage("Do you want to delete all data? Data can't be recovered after deletion.\n\nSome apps may have problems after this operation. If it is, please restart those apps");
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Yes",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                handleService(false);
                                new DatabaseDeleteAsyncTask().execute();
                            }
                        });
                alertDialog.show();

                return true;
            }
        });

    }

    void handleService(boolean opType) {
        Intent intent = new Intent(getApplicationContext(), ServiceDataKit.class);
        if (!opType) {
            if (Apps.isServiceRunning(getApplicationContext(), Constants.SERVICE_NAME)) {
                stopService(intent);
            }
        }
        else{
            startService(intent);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private ProgressDialog prgDialog;
    public static final int progress_bar_type = 0;

    class DatabaseDeleteAsyncTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Shows Progress Bar Dialog and then call doInBackground method
            showDialog(progress_bar_type);

        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                DatabaseLogger databaseLogger = DatabaseLogger.getInstance(getApplicationContext());
                assert databaseLogger != null;
                databaseLogger.removeAll();
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            // Dismiss the dialog after the Music file was downloaded
            dismissDialog(progress_bar_type);
//            handleService(true);
            setupPreferences();
            Toast.makeText(getApplicationContext(), "Database is Deleted", Toast.LENGTH_LONG).show();
        }
    }

    // Show Dialog Box with Progress bar
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case progress_bar_type:
                prgDialog = new ProgressDialog(this);
                prgDialog.setMessage("Deleting database. Please wait...");
                prgDialog.setIndeterminate(true);
                prgDialog.setCancelable(false);
                prgDialog.show();
                return prgDialog;
            default:
                return null;
        }
    }
}
