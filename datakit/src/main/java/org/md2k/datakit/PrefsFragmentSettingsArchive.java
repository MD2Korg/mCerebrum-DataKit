package org.md2k.datakit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.utilities.FileManager;
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
public class PrefsFragmentSettingsArchive extends PreferenceFragment {

    private static final String TAG = PrefsFragmentSettingsArchive.class.getSimpleName();
    Configuration configuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configuration = ConfigurationManager.getInstance(getActivity()).configuration;
        getPreferenceManager().getSharedPreferences().edit().clear().apply();
        getPreferenceManager().getSharedPreferences().edit().putBoolean("key_enabled",configuration.archive.enabled).apply();
        getPreferenceManager().getSharedPreferences().edit().putString("key_storage",configuration.archive.location).apply();
        getPreferenceManager().getSharedPreferences().edit().putString("key_interval",String.valueOf(configuration.archive.interval)).apply();
        addPreferencesFromResource(R.xml.pref_settings_archive);
        setBackButton();
        setSaveButton();
        if(getActivity().getIntent().getBooleanExtra("delete",false))
            clearArchive();
    }
    void clearArchive() {
        AlertDialogs.AlertDialog(getActivity(), "Delete Archive Files?", "Delete Archive Files?\n\nData can't be recovered after deletion",R.drawable.ic_delete_red_48dp, "Yes", "Cancel",null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    new ArchiveDeleteAsyncTask().execute();
                }else{
                    if(getActivity().getIntent().getBooleanExtra("delete",false))
                        getActivity().finish();
                }
            }
        });
    }
    void setupArchiveClear() {
        Preference preference = findPreference("key_delete");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                clearArchive();
                return true;
            }
        });
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
                configuration.archive.enabled = sharedPreferences.getBoolean("key_enabled", configuration.archive.enabled);
                configuration.archive.location = sharedPreferences.getString("key_storage", configuration.archive.location);
                configuration.archive.interval = Long.parseLong(sharedPreferences.getString("key_interval", String.valueOf(configuration.archive.interval)));
                if (configuration.archive.enabled == true && (configuration.archive.location == null || configuration.archive.interval == 0)) {
                    Toast.makeText(getActivity(), "Not Saved...not all values are set properly", Toast.LENGTH_LONG).show();
                    return;
                }
                ConfigurationManager.getInstance(getActivity()).write();
                setupPreferences();
                Toast.makeText(getActivity(),"Saved...",Toast.LENGTH_LONG).show();
            }
        });
    }


    void setupPreferences() {
        setupEnabled();
        setupStorage();
        setupDirectory();
        setupSize();
        setupSDCardSpace();
        setupArchiveClear();
    }
    void setupEnabled(){
        SwitchPreference switchPreference= (SwitchPreference) findPreference("key_enabled");
        boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean("key_enabled", configuration.archive.enabled);
        switchPreference.setChecked(enabled);
    }

    void setupSDCardSpace() {
        Preference preference = findPreference("key_sdcard_size");
        String location = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.archive.location);
        preference.setSummary(FileManager.getLocationType(getActivity(), location)+" ["+FileManager.getSDCardSizeString(getActivity(), location)+"]");
    }

    void setupSize() {
        Preference preference = findPreference("key_file_size");
        String location = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.archive.location);
        long fileSize = FileManager.getFileSize(getActivity(), location);
        preference.setSummary(FileManager.formatSize(fileSize));
    }

    void setupStorage() {
        ListPreference preference = (ListPreference) findPreference("key_storage");
        String storage=getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.archive.location);
        preference.setValue(storage);
        preference.setSummary(findString(getResources().getStringArray(R.array.sdcard_values), getResources().getStringArray(R.array.sdcard_text),storage));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getPreferenceManager().getSharedPreferences().edit().putString("key_storage",newValue.toString()).apply();
                setupPreferences();
                return false;
            }
        });
    }
    private String findString(String[] values, String[] strings, String value){
        for(int i=0;i<values.length;i++)
            if(values[i].equals(value))
                return strings[i];
        return ("(not selected)");
    }
    void setupDirectory() {
        Preference preference = findPreference("key_directory");
        String location = getPreferenceManager().getSharedPreferences().getString("key_storage", configuration.archive.location);
        String filename = FileManager.getDirectory(getActivity(), location) + Constants.ARCHIVE_DIRECTORY;
        preference.setSummary(filename);
    }
    class ArchiveDeleteAsyncTask extends AsyncTask<String, String, String> {
        private ProgressDialog dialog;

        ArchiveDeleteAsyncTask() {
            dialog = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Shows Progress Bar Dialog and then call doInBackground method
            dialog.setMessage("Deleting archive files. Please wait...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String location = ConfigurationManager.getInstance(getActivity()).configuration.archive.location;
                String filename = FileManager.getDirectory(getActivity(), location) + Constants.ARCHIVE_DIRECTORY;
                FileManager.deleteFile(filename);
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            setupPreferences();
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(getActivity(), "Archive files is Deleted", Toast.LENGTH_LONG).show();
            if(getActivity().getIntent().getBooleanExtra("delete",false))
                getActivity().finish();
        }
    }

}
