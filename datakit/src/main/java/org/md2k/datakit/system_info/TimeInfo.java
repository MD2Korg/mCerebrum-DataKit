package org.md2k.datakit.system_info;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeLongArray;
import org.md2k.datakitapi.messagehandler.OnConnectionListener;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.datakit.DataKitHandler;

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
public class TimeInfo {
    private static final String TAG = TimeInfo.class.getSimpleName();
    Context context;
    DataKitHandler dataKitHandler;
    public TimeInfo(final Context context){
        this.context=context;
        dataKitHandler=DataKitHandler.getInstance(context);
        dataKitHandler.connect(new OnConnectionListener() {
            @Override
            public void onConnected() {
                addData();
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
                context.registerReceiver(m_timeChangedReceiver, intentFilter);
            }
        });

    }
    DataSourceBuilder createDataSourceBuilder(){
        DataSourceBuilder dataSourceBuilder=new DataSourceBuilder();
        dataSourceBuilder.setType(DataSourceType.TIMEZONE);
        return dataSourceBuilder;
    }
    public void clear(){
        context.unregisterReceiver(m_timeChangedReceiver);
        dataKitHandler.disconnect();
        dataKitHandler.close();
    }
    private final BroadcastReceiver m_timeChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_CHANGED) ||
                    action.equals(Intent.ACTION_TIMEZONE_CHANGED))
            {
                Log.d(TAG,"broadcast receiver... TIMEZONE = changed");
                addData();
            }
        }
    };
    DataTypeLongArray createDataType(){
        long samples[]=new long[3];
        samples[0]= DateTime.getTimeZoneOffset();
        samples[1]=DateTime.getDayLightSavingOffset();
        samples[2]= DateTime.isDayLightSavingNow() ?1:0;
        Log.d(TAG," timezone values="+samples[0]+" "+ samples[1]+" "+samples[2]);
        return new DataTypeLongArray(DateTime.getDateTime(),samples);
    }
    boolean isExists(DataSourceClient dataSourceClient){
        ArrayList<DataType> dataTypes=dataKitHandler.query(dataSourceClient, 1);
        if(dataTypes==null || dataTypes.size()==0) return false;
        DataTypeLongArray dataTypeLongArray=(DataTypeLongArray)dataTypes.get(0);
        DataTypeLongArray curDataTypeLongArray=createDataType();
        for(int i=0;i<curDataTypeLongArray.getSample().length;i++)
            if(curDataTypeLongArray.getSample()[i]!=dataTypeLongArray.getSample()[i]) return false;
        return true;
    }
    void addData(){
        DataSourceBuilder dataSourceBuilder=createDataSourceBuilder();
        ArrayList<DataSourceClient> dataSourceClients=dataKitHandler.find(dataSourceBuilder);
        DataSourceClient dataSourceClient;
        if(dataSourceClients.size()==0)
            dataSourceClient=dataKitHandler.register(dataSourceBuilder);
        else dataSourceClient=dataSourceClients.get(0);
        if(!isExists(dataSourceClient)){
            Log.d(TAG, "add timezone data...");
            dataKitHandler.insert(dataSourceClient, createDataType());
        }
    }
}

