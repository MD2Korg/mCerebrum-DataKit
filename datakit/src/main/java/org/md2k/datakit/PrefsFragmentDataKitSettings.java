package org.md2k.datakit;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakit.operation.FileManager;
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
public class PrefsFragmentDataKitSettings extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_datakit_general);
        setupPreferences();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Defines the xml file for the fragment
//        View view = inflater.inflate(R.layout.fragment_foo, container, false);
        // Setup handles to view objects here
        // etFoo = (EditText) view.findViewById(R.id.etFoo);
//        return view;
        View v=super.onCreateView(inflater, container,savedInstanceState);
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);

        return v;
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
        preference.setSummary(FileManager.getValidSDcard(getActivity()));
    }
    void setupSDCardSpace(){
        Preference preference = findPreference("storage_space");
        preference.setSummary(FileManager.getStorageSpace(getActivity()));
    }
    void setupDatabaseSize(){
        Preference preference = findPreference("database_size");
        preference.setSummary(FileManager.getFileSize(getActivity()));
    }

    void setupDatabaseFile(){
        Preference preference = findPreference("database_filename");
        preference.setSummary(FileManager.getFilePath(getActivity()));
    }
    void setupDatabaseClear(){
        Preference preference = findPreference("database_clear");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                //TODO: verify the path for deletion of the database
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
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
        Intent intent = new Intent(getActivity(), ServiceDataKit.class);
        if (!opType) {
            if (Apps.isServiceRunning(getActivity(), Constants.SERVICE_NAME)) {
                getActivity().stopService(intent);
            }
        }
        else{
            getActivity().startService(intent);
        }

    }

    class DatabaseDeleteAsyncTask extends AsyncTask<String, String, String> {
        private ProgressDialog dialog;
        public static final int progress_bar_type = 0;
        DatabaseDeleteAsyncTask(){
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
                //TODO: verify then enable
//                DatabaseLogger databaseLogger = DatabaseLogger.getInstance(getActivity());
//                assert databaseLogger != null;
//                databaseLogger.removeAll();
            } catch (Exception e) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            // Dismiss the dialog after the Music file was downloaded
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
//            handleService(true);
            setupPreferences();
            Toast.makeText(getActivity(), "Database is Deleted", Toast.LENGTH_LONG).show();
        }
    }
}
