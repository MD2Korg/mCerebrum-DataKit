package org.md2k.datakit.privacy;

import android.content.Context;
import android.os.Handler;
import android.os.Messenger;
import android.util.SparseArray;

import com.google.gson.Gson;

import org.md2k.datakit.router.RoutingManager;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeString;
import org.md2k.datakitapi.source.application.Application;
import org.md2k.datakitapi.source.application.ApplicationBuilder;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.datakitapi.source.platform.Platform;
import org.md2k.datakitapi.source.platform.PlatformBuilder;
import org.md2k.datakitapi.source.platform.PlatformType;
import org.md2k.datakitapi.status.Status;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;

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
public class PrivacyManager {
    private static final String TAG = PrivacyManager.class.getSimpleName();
    private static PrivacyManager instance;
    Context context;
    RoutingManager routingManager;
    SparseArray<Boolean> listDsId;
    int dsIdPrivacy;
    Handler handler;
    PrivacyData lastPrivacyData;
    boolean active;

    public static PrivacyManager getInstance(Context context) throws IOException {
        if (instance == null)
            instance = new PrivacyManager(context);
        return instance;
    }

    private PrivacyManager(Context context) throws IOException {
        this.context = context;
        routingManager = RoutingManager.getInstance(context);
        active=false;
        listDsId = new SparseArray<>();
        handler = new Handler();
        processPrivacyData();
    }
    public boolean isActive(){
        return active;
    }

    private DataSource createDataSource(String dataSourceType, String platformType) {
        Platform platform;
        if (platformType == null || platformType.length() == 0)
            platform = null;
        else platform = new PlatformBuilder().setType(platformType).build();
        if (dataSourceType == null || dataSourceType.length() == 0)
            return new DataSourceBuilder().setPlatform(platform).build();
        else return new DataSourceBuilder().setType(dataSourceType).setPlatform(platform).build();
    }

    void createPrivacyList() {
        String dataSourceType;
        String platformType;
        listDsId.clear();
        int id;
        for (int i = 0; i < lastPrivacyData.privacyTypes.size(); i++) {
            for (int j = 0; j < lastPrivacyData.privacyTypes.get(i).source.size(); j++) {
                dataSourceType = lastPrivacyData.privacyTypes.get(i).source.get(j).datasource_type;
                platformType = lastPrivacyData.privacyTypes.get(i).source.get(j).platform_type;
                ArrayList<DataSourceClient> dataSourceClients = routingManager.find(createDataSource(dataSourceType, platformType));
                for (int k = 0; k < dataSourceClients.size(); k++) {
                    id = dataSourceClients.get(i).getDs_id();
                    listDsId.put(id, true);
                }
            }
        }
        listDsId.remove(dsIdPrivacy);
    }
    public DataSourceClient register(DataSource dataSource) {
        DataSourceClient dataSourceClient = routingManager.register(dataSource);
        createPrivacyList();
        return dataSourceClient;
    }

    public void insert(int ds_id, DataType dataType) {
        if (listDsId.get(ds_id) == null)
            routingManager.insert(ds_id, dataType);
        if(ds_id==dsIdPrivacy){
            processPrivacyData();
        }
    }
    public long getRemainingTime(){
        long currentTimeStamp = DateTime.getDateTime();
        Log.d(TAG,"duration="+lastPrivacyData.duration.getValue());
        long endTimeStamp = lastPrivacyData.startTimeStamp + lastPrivacyData.duration.getValue();
        return endTimeStamp-currentTimeStamp;

    }
    private void processPrivacyData(){
        lastPrivacyData= queryLastPrivacyData();
        Log.d(TAG, "lastPrivacyData=" + lastPrivacyData);
        if (lastPrivacyData == null|| !lastPrivacyData.status || getRemainingTime()<=0) {
            Log.d(TAG, "lastPrivacyData= deactivated");
            deactivate();
        }
        else {
            Log.d(TAG, "lastPrivacyData= activated");
            createPrivacyList();
            activate();
        }
    }

    public ArrayList<DataType> query(int ds_id, long starttimestamp, long endtimestamp) {
        return routingManager.query(ds_id, starttimestamp, endtimestamp);
    }

    public ArrayList<DataType> query(int ds_id, int last_n_sample) {
        return routingManager.query(ds_id, last_n_sample);
    }

    public Status unregister(int ds_id) {
        return routingManager.unregister(ds_id);
    }

    public Status subscribe(int ds_id, Messenger reply) {
        return routingManager.subscribe(ds_id, reply);
    }

    public Status unsubscribe(int ds_id, Messenger reply) {
        return routingManager.unsubscribe(ds_id, reply);
    }

    public ArrayList<DataSourceClient> find(DataSource dataSource) {
        return routingManager.find(dataSource);
    }

    private DataSource createDataSourcePrivacy() {
        Platform platform = new PlatformBuilder().setType(PlatformType.PHONE).build();
        Application application = new ApplicationBuilder().setId(context.getPackageName()).build();
        return new DataSourceBuilder().setType(DataSourceType.PRIVACY).setPlatform(platform).setApplication(application).build();
    }

    public void close() {
        listDsId.clear();
        routingManager.close();
        instance = null;
    }

    void activate() {
        handler.removeCallbacks(timer);
        Log.d(TAG,"remainingTime="+getRemainingTime());
        active=true;
        handler.postDelayed(timer,getRemainingTime());
    }
    public PrivacyData getLastPrivacyData(){
        return lastPrivacyData;
    }
    private PrivacyData queryLastPrivacyData() {
        Log.d(TAG, "queryLastPrivacyData");
        Gson gson = new Gson();
        dsIdPrivacy = routingManager.register(createDataSourcePrivacy()).getDs_id();
        Log.d(TAG, "ds_id="+dsIdPrivacy);

        ArrayList<DataType> dataTypes = routingManager.query(dsIdPrivacy, 1);
        Log.d(TAG, "DataTypes="+dataTypes+" size="+dataTypes.size());

        if (dataTypes == null || dataTypes.size() == 0) return null;
        return gson.fromJson(((DataTypeString) dataTypes.get(0)).getSample(), PrivacyData.class);
    }

    void deactivate() {
        listDsId.clear();
        active=false;
        handler.removeCallbacks(timer);
    }
    Runnable timer = new Runnable() {
        @Override
        public void run() {
            deactivate();
        }
    };
}
