package org.md2k.datakit;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakit.logger.DatabaseLogger;
import org.md2k.datakit.operation.FileManager;
import org.md2k.utilities.Apps;
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
public class PrefsFragmentDataKitSettings extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.pref_datakit_general);
        setupPreferences();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v=super.onCreateView(inflater, container,savedInstanceState);
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

    void setupPreferences() {
        setupDatabaseFile();
        setupDatabaseLocation();
        setupDatabaseClear();
        setupSDCardSpace();
        setupDatabaseSize();
        setBackButton();
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
                clearDatabase();
                return true;
            }
        });
    }
    void sendLocalBroadcast(String str){
        Intent intent = new Intent("datakit");
        intent.putExtra("action", str);
        LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);

    }
    void clearDatabase(){
        AlertDialogs.showAlertDialogConfirm(getActivity(), "Clear Database", "Clear Database?\n\nData can't be recovered after deletion\n\nSome apps may have problems after this operation. If it is, please restart those apps", "Yes", "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    sendLocalBroadcast("stop");
                    new DatabaseDeleteAsyncTask().execute();
                }
            }
        });
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
                DatabaseLogger databaseLogger = DatabaseLogger.getInstance(getActivity());
                assert databaseLogger != null;
                databaseLogger.removeAll();
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
        }
    }
}
