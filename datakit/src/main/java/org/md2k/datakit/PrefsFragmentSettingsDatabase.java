package org.md2k.datakit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.utilities.FileManager;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;

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
public class PrefsFragmentSettingsDatabase extends PreferenceFragment {
    private static final String TAG = PrefsFragmentSettingsDatabase.class.getSimpleName();
    Configuration configuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configuration = ConfigurationManager.getInstance(getActivity()).configuration;
        Log.d(TAG, "configuration=" + configuration);
        getPreferenceManager().getSharedPreferences().edit().clear().apply();
        getPreferenceManager().getSharedPreferences().edit().putString("key_storage", configuration.database.location).apply();
        addPreferencesFromResource(R.xml.pref_settings_database);
        setBackButton();
        setSaveButton();
        if (getActivity().getIntent().getBooleanExtra("delete", false))
            clearDatabase();
    }

    @Override
    public void onResume() {
        setupPreferences();
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);

        return v;
    }

    private void setBackButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Close");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    private void setSaveButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText("Save");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
                configuration.database.location = sharedPreferences.getString("key_storage", configuration.database.location);
                ConfigurationManager.getInstance(getActivity()).write();
                Toast.makeText(getActivity(), "Saved...", Toast.LENGTH_LONG).show();
                setupPreferences();
            }
        });
    }

    void setupPreferences() {
        setupStorage();
        setupDatabaseFile();
        setupDatabaseClear();
        setupSDCardSpace();
        setupDatabaseSize();
    }

    void setupStorage() {
        ListPreference preference = (ListPreference) findPreference("key_storage");
        String storage = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.database.location);
        preference.setValue(storage);
        Log.d(TAG, "shared=" + storage + " config=" + configuration.database.location);
        preference.setSummary(findString(getResources().getStringArray(R.array.sdcard_values), getResources().getStringArray(R.array.sdcard_text), storage));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getPreferenceManager().getSharedPreferences().edit().putString("key_storage", newValue.toString()).apply();
                setupPreferences();
                return false;
            }
        });
    }

    private String findString(String[] values, String[] strings, String value) {
        for (int i = 0; i < values.length; i++)
            if (values[i].equals(value))
                return strings[i];
        return ("(not selected)");
    }

    void setupSDCardSpace() {
        Preference preference = findPreference("key_sdcard_size");
        String location = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.database.location);
        preference.setSummary(FileManager.getLocationType(getActivity(), location) + " [" + FileManager.getSDCardSizeString(getActivity(), location) + "]");
    }

    void setupDatabaseSize() {
        Preference preference = findPreference("key_file_size");
        String location = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.database.location);
        long fileSize = FileManager.getFileSize(getActivity(), location);
        preference.setSummary(FileManager.formatSize(fileSize));
    }

    void setupDatabaseFile() {
        Preference preference = findPreference("key_directory");
        String location = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.database.location);
        String filename = FileManager.getDirectory(getActivity(), location) + Constants.DATABASE_FILENAME;
        preference.setSummary(filename);
    }

    void setupDatabaseClear() {
        Preference preference = findPreference("key_delete");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                clearDatabase();
                return true;
            }
        });
    }

    void sendLocalBroadcast(String str) {
        Intent intent = new Intent("datakit");
        intent.putExtra("action", str);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
    }

    void clearDatabase() {
        AlertDialogs.AlertDialog(getActivity(), "Clear Database", "Clear Database?\n\nData can't be recovered after deletion\n\nSome apps may have problems after this operation. If it is, please restart those apps", R.drawable.ic_delete_red_48dp, "Yes", "Cancel", null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    sendLocalBroadcast("stop");
                    new DatabaseDeleteAsyncTask().execute();
                } else {
                    if (getActivity().getIntent().getBooleanExtra("delete", false))
                        getActivity().finish();
                }
            }
        });
    }

    class DatabaseDeleteAsyncTask extends AsyncTask<String, String, String> {
        private ProgressDialog dialog;

        DatabaseDeleteAsyncTask() {
            dialog = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Shows Progress Bar Dialog and then call doInBackground method
            dialog.setMessage("Deleting database. Please wait...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String location = ConfigurationManager.getInstance(getActivity()).configuration.database.location;
                String filename = FileManager.getDirectory(getActivity(), location) + Constants.DATABASE_FILENAME;
                FileManager.deleteFile(filename);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            // Dismiss the dialog after the Music file was downloaded
            sendLocalBroadcast("start");
            setupPreferences();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(getActivity(), "Database is Deleted", Toast.LENGTH_LONG).show();
            if (getActivity().getIntent().getBooleanExtra("delete", false))
                getActivity().finish();
        }
    }
}
