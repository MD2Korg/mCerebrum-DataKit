package org.md2k.datakit;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
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
public class PrefsFragmentSettingsUpload extends PreferenceFragment {

    private static final String TAG = PrefsFragmentSettingsUpload.class.getSimpleName();
    Configuration configuration;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configuration=ConfigurationManager.getInstance(getActivity()).configuration;
        getPreferenceManager().getSharedPreferences().edit().clear().apply();
        getPreferenceManager().getSharedPreferences().edit().putBoolean("key_enabled",configuration.upload.enabled).apply();
        getPreferenceManager().getSharedPreferences().edit().putString("key_url",configuration.upload.url).apply();
        getPreferenceManager().getSharedPreferences().edit().putString("key_interval",String.valueOf(configuration.upload.interval)).apply();
        addPreferencesFromResource(R.xml.pref_settings_upload);
        setBackButton();
        setSaveButton();
    }
    @Override
    public void onResume(){
        setupPreferences();
        super.onResume();
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
    void setupPreferences(){
        setupEnabled();
        setupInterval();
        setupURL();
    }
    void setupEnabled(){
        SwitchPreference switchPreference= (SwitchPreference) findPreference("key_enabled");
        boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean("key_enabled", configuration.upload.enabled);
        switchPreference.setChecked(enabled);
    }
    void setupInterval() {
        ListPreference preference = (ListPreference) findPreference("key_interval");
        String interval = getPreferenceManager().getSharedPreferences().getString("key_interval", String.valueOf(configuration.upload.interval));
        preference.setValue(interval);
        preference.setSummary(findString(getResources().getStringArray(R.array.upload_interval_values), getResources().getStringArray(R.array.upload_interval_text), interval));
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getPreferenceManager().getSharedPreferences().edit().putString("key_interval", newValue.toString()).apply();
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

    private void setSaveButton() {
        final Button button = (Button) getActivity().findViewById(R.id.button_2);
        button.setText("Save");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
                configuration.upload.enabled = sharedPreferences.getBoolean("key_enabled", configuration.upload.enabled);
                configuration.upload.url = sharedPreferences.getString("key_url", configuration.upload.url);
                configuration.upload.interval = Long.parseLong(sharedPreferences.getString("key_interval", String.valueOf(configuration.upload.interval)));
                if (configuration.upload.enabled == true && (configuration.upload.url == null || configuration.upload.interval == 0)) {
                    Toast.makeText(getActivity(), "Not Saved...not all values are set properly", Toast.LENGTH_LONG).show();
                    return;
                }
                ConfigurationManager.getInstance(getActivity()).write();
                Toast.makeText(getActivity(),"Saved...",Toast.LENGTH_LONG).show();

            }
        });
    }
    void setupURL() {
        EditTextPreference preference = (EditTextPreference) findPreference("key_url");
        String url = getPreferenceManager().getSharedPreferences().getString("key_url", configuration.upload.url);
        preference.setText(url);
        preference.setSummary(url);
        preference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                getPreferenceManager().getSharedPreferences().edit().putString("key_url", newValue.toString()).apply();
                setupPreferences();
                return false;
            }
        });
    }

}
