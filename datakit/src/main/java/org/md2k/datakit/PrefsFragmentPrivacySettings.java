package org.md2k.datakit;

import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.datakit.configuration.PrivacyConfig;
import org.md2k.datakit.privacy.PrivacyManager;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.commons.dialog.Dialog;
import org.md2k.mcerebrum.commons.dialog.DialogCallback;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.data_format.privacy.Duration;
import org.md2k.utilities.data_format.privacy.PrivacyData;
import org.md2k.utilities.data_format.privacy.PrivacyType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

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
public class PrefsFragmentPrivacySettings extends PreferenceFragment {
    private static final String TAG = PrefsFragmentPrivacySettings.class.getSimpleName();
    PrivacyConfig privacyConfig;
    Handler handler;
    PrivacyData newPrivacyData;
    PrivacyManager privacyManager;
    long remainingTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate...");
        if(getActivity().getIntent().hasExtra("REMAINING_TIME")){
            remainingTime=getActivity().getIntent().getLongExtra("REMAINING_TIME",Long.MAX_VALUE);
        }else{
            remainingTime=Long.MAX_VALUE;
        }
        getPreferenceManager().getSharedPreferences().edit().clear().apply();
        privacyConfig=ConfigurationManager.getInstance(getActivity()).configuration.privacy;
        newPrivacyData=new PrivacyData();
        handler = new Handler();
        addPreferencesFromResource(R.xml.pref_privacy);
        try {
            privacyManager=PrivacyManager.getInstance(getActivity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        setupButtonSaveCancel();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        LinearLayout v = (LinearLayout) super.onCreateView(inflater, container, savedInstanceState);
        assert v != null;
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setPadding(0, 0, 0, 0);
        return v;
    }

    void setupButtonSaveCancel() {
        final Button buttonStartStop = (Button) getActivity().findViewById(R.id.button_1);
        Button buttonCancel = (Button) getActivity().findViewById(R.id.button_2);

        buttonStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(privacyManager.isActive()){
                    PrivacyData privacyData=privacyManager.getPrivacyData();
                    privacyData.setStatus(false);
                    privacyManager.insertPrivacy(privacyData);
                    Toast.makeText(getActivity(), "Privacy Mode Off...", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                } else {
                    if(preparePrivacyData()) {
                        privacyManager.insertPrivacy(newPrivacyData);
                        Toast.makeText(getActivity(), "Privacy Mode On...", Toast.LENGTH_SHORT).show();
                        getActivity().finish();
                    }
                }
            }
        });
        buttonCancel.setText("Close");
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().finish();
            }
        });
    }

    boolean preparePrivacyData() {
        if(newPrivacyData.getDuration()==null) {
            Dialog.simple(getActivity(), "ERROR: Duration", "Duration is not set", "Ok", null, new DialogCallback() {
                @Override
                public void onSelected(String value) {

                }
            }).show();
            return false;
        }
        else if (newPrivacyData.getPrivacyTypes() == null || newPrivacyData.getPrivacyTypes().size() == 0) {
            Dialog.simple(getActivity(), "ERROR: Privacy Type", "Privacy Type is not selected", "Ok", null, new DialogCallback() {
                @Override
                public void onSelected(String value) {

                }
            }).show();
            return false;
        }
        else {
            newPrivacyData.setStartTimeStamp(DateTime.getDateTime());
            newPrivacyData.setStatus(true);
            return true;
        }
    }

    void updateUI() {
        Preference preference = findPreference("status");
        preference.setEnabled(false);
        PreferenceCategory pc = (PreferenceCategory) findPreference("category_settings");

        if (privacyManager.isActive()) {
            ((Button) getActivity().findViewById(R.id.button_1)).setText("Stop");
            pc.setEnabled(false);
            Spannable summary = new SpannableString("ON (" + DateTime.convertTimestampToTimeStr(privacyManager.getRemainingTime()) + ")");
            summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.red_700)), 0, summary.length(), 0);
            preference.setSummary(summary);
        } else {
            ((Button) getActivity().findViewById(R.id.button_1)).setText("Start");
            pc.setEnabled(true);
            Spannable summary = new SpannableString("OFF");
            summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.teal_700)), 0, summary.length(), 0);
            preference.setSummary(summary);
        }
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                updateUI();
                handler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onResume() {
        handler.post(runnable);
        super.onResume();
    }

    @Override
    public void onPause() {
        handler.removeCallbacks(runnable);
        super.onPause();
    }
    @Override
    public void onStart(){
        setupPreferences();
        super.onStart();
    }

    void setupPreferences() {
        setupDuration();
        setupPrivacyType();
    }

    void setupPrivacyType() {
        final ArrayList<PrivacyType> privacyTypes = privacyConfig.privacy_type_options;
        final MultiSelectListPreference listPreference = (MultiSelectListPreference) findPreference("privacy_type");
        String[] entries = new String[privacyTypes.size()];
        String[] entryValues = new String[privacyTypes.size()];
        for (int i = 0; i < privacyTypes.size(); i++) {
            entries[i] = privacyTypes.get(i).getTitle();
            entryValues[i] = privacyTypes.get(i).getId();
        }
        listPreference.setEntries(entries);
        listPreference.setEntryValues(entryValues);
        if (privacyManager.isActive()) {
            String list="";
            for(int i=0;i<privacyManager.getPrivacyData().getPrivacyTypes().size();i++){
                if(!list.equals("")) list+=", ";
                list+=privacyManager.getPrivacyData().getPrivacyTypes().get(i).getTitle();
            }
            listPreference.setSummary(list);
        } else {
            Spannable summary = new SpannableString("(Click Here)");
            summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.red_700)), 0, summary.length(), 0);
            listPreference.setSummary(summary);
        }
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                ArrayList<PrivacyType> privacyTypeSelected= new ArrayList<>();
                String list="";
                for (int i = 0; i < privacyTypes.size(); i++){
                    if(((HashSet)newValue).contains(privacyTypes.get(i).getId())) {
                        privacyTypeSelected.add(privacyTypes.get(i));
                        if (!list.equals(""))
                            list += ", ";
                        list = list + privacyTypes.get(i).getTitle();
                    }
                }
                listPreference.setSummary(list);
                newPrivacyData.setPrivacyTypes(privacyTypeSelected);
                return false;
            }
        });
    }

    void setupDuration() {
        final ArrayList<Duration> durations = privacyConfig.duration_options;
        final ListPreference listPreference = (ListPreference) findPreference("duration");
        ArrayList<String> entries = new ArrayList<>();
        ArrayList<String> entryValues = new ArrayList<>();
        for (int i = 0; i < durations.size(); i++) {
            if(durations.get(i).getValue()<=remainingTime) {
                entries.add(durations.get(i).getTitle());
                entryValues.add(durations.get(i).getId());
            }
        }
        listPreference.setEntries(entries.toArray(new String[entries.size()]));
        listPreference.setEntryValues(entryValues.toArray(new String[entryValues.size()]));
        if (privacyManager.isActive()) {
            listPreference.setSummary(privacyManager.getPrivacyData().getDuration().getTitle());

        } else {
            Spannable summary = new SpannableString("(Click Here)");
            summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.red_700)), 0, summary.length(), 0);
            listPreference.setSummary(summary);
        }
        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int index = listPreference.findIndexOfValue(newValue.toString());
                if (index >= 0) {
                    preference.setSummary(listPreference.getEntries()[index]);
                    listPreference.setValueIndex(index);
                    for (int i = 0; i < durations.size(); i++)
                        if (durations.get(i).getId().equals(newValue))
                            newPrivacyData.setDuration(durations.get(i));
                }
                return false;
            }
        });
    }

}
