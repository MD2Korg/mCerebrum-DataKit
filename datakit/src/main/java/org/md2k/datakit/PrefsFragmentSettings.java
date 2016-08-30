package org.md2k.datakit;

import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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
public class PrefsFragmentSettings extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings);
        setBackButton();
        setPreferences();
        if(getActivity().getIntent().getBooleanExtra("delete",false))
            clearData();
    }
    void clearData() {
        AlertDialogs.AlertDialog(getActivity(), "Delete Archive Files?", "Delete Database & Archive Files?\n\nData can't be recovered after deletion",R.drawable.ic_delete_red_48dp, "Yes", "Cancel",null, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == AlertDialog.BUTTON_POSITIVE) {
                    new DeleteDataAsyncTask().execute();
                }else{
                    if(getActivity().getIntent().getBooleanExtra("delete",false))
                        getActivity().finish();
                }
            }
        });
    }
    class DeleteDataAsyncTask extends AsyncTask<String, String, String> {
        private ProgressDialog dialog;

        DeleteDataAsyncTask() {
            dialog = new ProgressDialog(getActivity());
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Shows Progress Bar Dialog and then call doInBackground method
            dialog.setMessage("Deleting database & archive files. Please wait...");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String location = ConfigurationManager.getInstance(getActivity()).configuration.archive.location;
                String directory = FileManager.getDirectory(getActivity(), location);
                FileManager.deleteDirectory(directory);
                location = ConfigurationManager.getInstance(getActivity()).configuration.database.location;
                directory = FileManager.getDirectory(getActivity(), location);
                FileManager.deleteDirectory(directory);
            } catch (Exception ignored) {
            }
            return null;
        }

        @Override
        protected void onPostExecute(String file_url) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            Toast.makeText(getActivity(), "Archive files is Deleted", Toast.LENGTH_LONG).show();
            if(getActivity().getIntent().getBooleanExtra("delete",false))
                getActivity().finish();
            else
                setPreferences();

        }
    }

    public void setPreferences(){
        setPreferenceDatabase();
        setPreferenceArchive();
        setPreferenceUpload();
    }
    void setPreferenceDatabase(){
        Preference preference=findPreference("key_database");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent=new Intent(getActivity(),ActivitySettingsDatabase.class);
                startActivity(intent);
                return false;
            }
        });
    }
    void setPreferenceArchive(){
        Preference preference=findPreference("key_archive");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent=new Intent(getActivity(),ActivitySettingsArchive.class);
                startActivity(intent);
                return false;
            }
        });
    }
    void setPreferenceUpload(){
        Preference preference=findPreference("key_upload");
        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent=new Intent(getActivity(),ActivitySettingsUpload.class);
                startActivity(intent);
                return false;
            }
        });
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v=super.onCreateView(inflater, container,savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);

        return v;
    }
    private void setBackButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText(R.string.button_close);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }
}
