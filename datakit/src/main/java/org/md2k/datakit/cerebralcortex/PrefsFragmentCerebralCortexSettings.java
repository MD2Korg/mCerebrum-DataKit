package org.md2k.datakit.cerebralcortex;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakit.R;
import org.md2k.datakit.cerebralcortex.config.Config;
import org.md2k.datakit.cerebralcortex.config.ConfigManager;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.utilities.Apps;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;

import java.io.FileNotFoundException;
import java.io.IOException;

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

public class PrefsFragmentCerebralCortexSettings extends PreferenceFragment {
    private static final String TAG = PrefsFragmentCerebralCortexSettings.class.getSimpleName();
    Config defaultConfig;
    Config config;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref_settings_upload);
        try {
            defaultConfig = ConfigManager.readDefaultConfig();
        } catch (FileNotFoundException e) {
            defaultConfig = null;
        }
        try {
            config = ConfigManager.readConfig();
        } catch (FileNotFoundException e) {
            config = new Config();
        }
        if (defaultConfig != null) config = defaultConfig;
        createPrefInterval();
        createPrefHistory();
        createPrefAPIURL();
        createPrefRestrictLocation();
        setBackButton();
        setSaveButton();
    }

    void createPrefRestrictLocation() {
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference("restrict_location");
        if (defaultConfig != null) {
            checkBoxPreference.setEnabled(false);
        } else {
            checkBoxPreference.setEnabled(true);
        }
        if (config.isDataSourceExist(DataSourceType.LOCATION))
            checkBoxPreference.setChecked(true);
        else {
            checkBoxPreference.setChecked(false);
        }
        checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Log.d(TAG, "check=" + newValue);
                if ((Boolean) newValue) {
                    config.addDataSource(DataSourceType.LOCATION);
                    ((CheckBoxPreference) preference).setChecked(true);
                } else {
                    config.removeDataSource(DataSourceType.LOCATION);
                    ((CheckBoxPreference) preference).setChecked(false);
                }
                return false;
            }
        });
    }

    String convertTimeToString(long timestamp) {
        String timeStr = "";
        timestamp /= 1000;
        if (timestamp != 0 && timestamp % 60 != 0) {
            timeStr = String.valueOf(timestamp % 60) + " Second " + timeStr;
        }
        timestamp /= 60;
        if (timestamp != 0 && timestamp % 60 != 0) {
            timeStr = String.valueOf(timestamp % 60) + " Minute " + timeStr;
        }
        timestamp /= 60;
        if (timestamp != 0) {
            timeStr = String.valueOf(timestamp) + " Hour " + timeStr;
        }
        return timeStr;
    }

    void createPrefInterval() {
        final ListPreference listPreference = (ListPreference) findPreference("upload_interval");
        if (defaultConfig != null) {
            listPreference.setEnabled(false);
        } else {
            listPreference.setEnabled(true);
        }
        if (config.getUpload_interval() == 0)
            listPreference.setSummary("(not assigned)");
        else {
            listPreference.setSummary(convertTimeToString(config.getUpload_interval()));
        }
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ("custom".equals(newValue)) {
                    showEditDialog(preference);
                } else {
                    config.setUpload_interval(Long.parseLong(newValue.toString()));
                    listPreference.setSummary(convertTimeToString(config.getUpload_interval()));
                }
                return false;

            }
        });
    }

    void createPrefHistory() {
        final ListPreference listPreferenceHistory = (ListPreference) findPreference("history_time");
        if (defaultConfig != null) {
            listPreferenceHistory.setEnabled(false);
        } else {
            listPreferenceHistory.setEnabled(true);
        }
        if (config.getHistory_time() == 0)
            listPreferenceHistory.setSummary("(not assigned)");
        else {
            listPreferenceHistory.setSummary(convertTimeToString(config.getHistory_time()));
        }
        listPreferenceHistory.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ("custom".equals(newValue)) {
                    showHistoryEditDialog(preference);
                } else {
                    config.setHistory_time(Long.parseLong(newValue.toString()));
                    listPreferenceHistory.setSummary(convertTimeToString(config.getHistory_time()));
                }
                return false;

            }
        });
    }

    void showEditDialog(final Preference preference) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Upload Interval (in Minutes)");

        final EditText input = new EditText(getActivity());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                config.setUpload_interval(Long.parseLong(input.getText().toString()) * 60 * 1000);
                preference.setSummary(convertTimeToString(config.getUpload_interval()));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    void showHistoryEditDialog(final Preference preference) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("History to keep (in Minutes)");

        final EditText input = new EditText(getActivity());
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                config.setHistory_time(Long.parseLong(input.getText().toString()) * 60 * 1000);
                preference.setSummary(convertTimeToString(config.getHistory_time()));
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    void createPrefAPIURL() {
        EditTextPreference editTextPreference = (EditTextPreference) findPreference("api_url");
        if (defaultConfig != null) {
            editTextPreference.setEnabled(false);
        } else {
            editTextPreference.setEnabled(true);
        }
        if (config.getUrl() == null || config.getUrl().length() == 0)
            editTextPreference.setSummary("(not assigned)");
        else {
            editTextPreference.setSummary(config.getUrl());
        }

        editTextPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                String temp = (String) newValue;
                if (temp == null || temp.length() == 0) return false;
                config.setUrl(temp);
                preference.setSummary(temp);
                return false;
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);
        return v;
    }

    private void setBackButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText("Close");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    private void setSaveButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_1);
        button.setText("Save");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Apps.isServiceRunning(getActivity(), ServiceCerebralCortex.class.getName())) {
                    AlertDialogs.AlertDialog(getActivity(), "Save and Restart?", "Save configuration file and restart Data Uploader App?", R.drawable.ic_info_teal_48dp, "Yes", "Cancel", null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_POSITIVE:
                                    Intent intent = new Intent(getActivity(), ServiceCerebralCortex.class);
                                    getActivity().stopService(intent);
                                    saveConfigurationFile();
                                    intent = new Intent(getActivity(), ServiceCerebralCortex.class);
                                    getActivity().startService(intent);
                                    getActivity().finish();
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    Toast.makeText(getActivity(), "!!! Error: Configuration file is not saved.", Toast.LENGTH_LONG).show();
                                    getActivity().finish();
                                    break;
                            }
                        }
                    });
                } else {
                    saveConfigurationFile();
                    getActivity().finish();
                }
            }
        });
    }

    void saveConfigurationFile() {
        try {
            ConfigManager.write(config);
            Toast.makeText(getActivity(), "Configuration saved", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(getActivity(), "!!!Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
