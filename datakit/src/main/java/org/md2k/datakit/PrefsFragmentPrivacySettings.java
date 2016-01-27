package org.md2k.datakit;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
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

import org.md2k.datakit.privacy.Duration;
import org.md2k.datakit.privacy.PrivacyController;
import org.md2k.datakit.privacy.PrivacyData;
import org.md2k.datakit.privacy.PrivacyType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.UI.AlertDialogs;

import java.io.IOException;
import java.util.ArrayList;

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
    PrivacyController privacyController;
    Handler handler;
    Duration durationSelected;
    ArrayList<PrivacyType> privacyTypeSelected=new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate...");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.clear();
        editor.commit();
        try {
            privacyController = PrivacyController.getInstance(getActivity());
            if(!privacyController.isAvailable()) getActivity().finish();
        } catch (IOException e) {
            //TODO: show alert dialog
            getActivity().finish();
        }
        handler = new Handler();
        addPreferencesFromResource(R.xml.pref_privacy);
        setupPreferences();
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
                try {
                    PrivacyData privacyData;
                    if (privacyController.isActive()) {
                        privacyData= privacyController.getPrivacyData();
                        durationSelected=privacyData.getDuration();
                        privacyTypeSelected=privacyData.getPrivacyTypes();

                        privacyData.setStatus(false);
                        PrivacyController.getInstance(getActivity()).insertPrivacyData(privacyData);
                    } else {
                        privacyData=preparePrivacyData();
                        if(privacyData!=null){
                            PrivacyController.getInstance(getActivity()).insertPrivacyData(privacyData);
                            Toast.makeText(getActivity(), "Data collection stopped temporarily...", Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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
    PrivacyData preparePrivacyData(){
        PrivacyData privacyData=null;
        if(durationSelected==null)
               AlertDialogs.showAlertDialog(getActivity(), "ERROR: Duration", "Duration is not set");
        else if(privacyTypeSelected==null || privacyTypeSelected.size()==0)
            AlertDialogs.showAlertDialog(getActivity(), "ERROR: Privacy Type", "Privacy Type is not selected");
        else{
            privacyData=new PrivacyData();
            privacyData.setDuration(durationSelected);
            privacyData.setStartTimeStamp(DateTime.getDateTime());
            privacyData.setStatus(true);
            privacyData.setPrivacyTypes(privacyTypeSelected);
        }
        return privacyData;
    }
    void updateUI(){
        Preference preference=findPreference("status");
        PreferenceCategory pcDuration= (PreferenceCategory) findPreference("category_duration");
        PreferenceCategory pcType= (PreferenceCategory) findPreference("category_privacy_type");

        if (privacyController.isActive()) {
            ((Button)getActivity().findViewById(R.id.button_1)).setText("Stop");
            pcDuration.setEnabled(false);
            pcType.setEnabled(false);
            Spannable summary = new SpannableString("ON (" + DateTime.convertTimestampToTimeStr(privacyController.getRemainingTime()) + ")");
            summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(),R.color.red_700)), 0, summary.length(), 0);
            preference.setSummary(summary);        }
        else {
            ((Button)getActivity().findViewById(R.id.button_1)).setText("Start");
            pcDuration.setEnabled(true);
            pcType.setEnabled(true);
            Spannable summary = new SpannableString("OFF");
            summary.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getActivity(), R.color.teal_700)), 0, summary.length(), 0);
            preference.setSummary(summary);
        }
    }
    Handler mHandler = new Handler();
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            {
                updateUI();
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    public void onResume() {
        mHandler.post(runnable);
        super.onResume();
    }

    @Override
    public void onPause() {
        mHandler.removeCallbacks(runnable);
        super.onPause();
    }


    void setupPreferences() {
        setupDuration();
        setupPrivacyType();
    }


    void setupPrivacyType() {
        final ArrayList<PrivacyType> privacyTypes = privacyController.getPrivacyConfiguration().getPrivacyType();
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("category_privacy_type");
        preferenceCategory.removeAll();
        for (int i = 0; i < privacyTypes.size(); i++) {
            CheckBoxPreference checkBoxPreference = new CheckBoxPreference(getActivity());
            checkBoxPreference.setTitle(privacyTypes.get(i).getTitle());
            checkBoxPreference.setSummary(privacyTypes.get(i).getSummary());
            checkBoxPreference.setKey(privacyTypes.get(i).getId());
            final int finalI = i;
            checkBoxPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ((CheckBoxPreference) preference).setChecked((Boolean) newValue);
                    Log.d(TAG, "newvalue=" + newValue.toString());
                    if (newValue == true) {
                        privacyTypeSelected.add(privacyTypes.get(finalI));
                    } else {
                        privacyTypeSelected.remove(privacyTypes.get(finalI));
                    }
                    for(int i=0;i<privacyTypeSelected.size();i++)
                        Log.d(TAG,"i="+i+" "+privacyTypeSelected.get(i).getId());
                    return (boolean) newValue;
                }
            });
            if(privacyController.isActive() && privacyController.isPrivacyTypeExists(privacyTypes.get(i).getId())) {
                checkBoxPreference.setChecked(true);
                privacyTypeSelected.add(privacyTypes.get(i));
            }
            else checkBoxPreference.setChecked(false);
            preferenceCategory.addPreference(checkBoxPreference);
        }
    }

    void setupDuration() {
        final ArrayList<Duration> durations = privacyController.getPrivacyConfiguration().getDuration();
        final ListPreference listPreference = (ListPreference) findPreference("duration");
        String[] entries = new String[durations.size()];
        String[] entryValues = new String[durations.size()];
        for (int i = 0; i < durations.size(); i++) {
            entries[i] = durations.get(i).getTitle();
            entryValues[i] = durations.get(i).getId();
        }
        listPreference.setEntries(entries);
        listPreference.setEntryValues(entryValues);
        if (privacyController.isActive()) {
            listPreference.setSummary(privacyController.getPrivacyData().getDuration().getTitle());
//            listPreference.setValueIndex(listPreference.findIndexOfValue(privacyController.getPrivacyData().getDuration().getTitle()));

        } else {
            listPreference.setSummary("");
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
                            durationSelected = durations.get(i);
                } else preference.setSummary("");
                return false;
            }
        });
    }

}
