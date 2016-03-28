package org.md2k.datakit.privacy;

import android.content.Context;
import android.os.Handler;
import android.os.Messenger;
import android.util.SparseArray;

import com.google.gson.Gson;

import org.md2k.datakit.router.RoutingManager;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.DataTypeString;
import org.md2k.datakitapi.datatype.RowObject;
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

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * - Timothy W. Hnat <twhnat@memphis.edu>
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
public class PrivacyManager {
    private static final String TAG = PrivacyManager.class.getSimpleName();
    //    private static final String TAG = PrivacyManager.class.getSimpleName();
    private static PrivacyManager instance;
    Context context;
    RoutingManager routingManager;
    SparseArray<Boolean> listPrivacyListDsId;
    int dsIdPrivacy;
    Handler handler;
    PrivacyData lastPrivacyData;
    boolean active;
    Runnable timer = new Runnable() {
        @Override
        public void run() {
            deactivate();
        }
    };

    private PrivacyManager(Context context) throws IOException {
        Log.d(TAG,"PrivacyManager()..constructor()..");
        this.context = context;
        routingManager = RoutingManager.getInstance(context);
        active=false;
        listPrivacyListDsId = new SparseArray<>();
        handler = new Handler();
        processPrivacyData();
    }

    public static PrivacyManager getInstance(Context context) throws IOException {
        if (instance == null)
            instance = new PrivacyManager(context);
        return instance;
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
        Log.d(TAG,"createPrivacyList()...");
        if(lastPrivacyData==null || lastPrivacyData.privacyTypes==null) return;
        listPrivacyListDsId.clear();
        String dataSourceType;
        String platformType;
        int id;
        for (int i = 0; i < lastPrivacyData.privacyTypes.size(); i++) {
            for (int j = 0; j < lastPrivacyData.privacyTypes.get(i).source.size(); j++) {
                dataSourceType = lastPrivacyData.privacyTypes.get(i).source.get(j).datasource_type;
                platformType = lastPrivacyData.privacyTypes.get(i).source.get(j).platform_type;
                Log.d(TAG,"search...dataSourceType="+dataSourceType+" platformType="+platformType);
                ArrayList<DataSourceClient> dataSourceClients = routingManager.find(createDataSource(dataSourceType, platformType));
                for (int k = 0; k < dataSourceClients.size(); k++) {
                    Log.d(TAG,"id="+dataSourceClients.get(k).getDs_id());
                    id = dataSourceClients.get(k).getDs_id();
                    listPrivacyListDsId.put(id, true);
                }
            }
        }
        listPrivacyListDsId.remove(dsIdPrivacy);
    }

    public DataSourceClient register(DataSource dataSource) {
        DataSourceClient dataSourceClient = routingManager.register(dataSource);
        createPrivacyList();
        return dataSourceClient;
    }

    public Status insert(int ds_id, DataType dataType) {
        Status status=new Status(Status.SUCCESS);
        if(ds_id==-1 || dataType==null)
            return new Status(Status.INTERNAL_ERROR);
        if (listPrivacyListDsId.get(ds_id) == null) {
            status = routingManager.insert(ds_id, dataType);
            if(status.getStatusCode()==Status.INTERNAL_ERROR)
                return status;
        }
        if(ds_id==dsIdPrivacy){
            Log.d(TAG,"privacy data...process start...");
            processPrivacyData();
        }
        return status;
    }

    public Status insertHF(int ds_id, DataTypeDoubleArray dataType) {
        Status status = new Status(Status.SUCCESS);
        if (ds_id == -1 || dataType == null)
            return new Status(Status.INTERNAL_ERROR);
        if (listPrivacyListDsId.get(ds_id) == null) {
            status = routingManager.insertHF(ds_id, dataType);
            if (status.getStatusCode() == Status.INTERNAL_ERROR)
                return status;
        }
        if (ds_id == dsIdPrivacy) {
            Log.d(TAG, "privacy data...process start...");
            processPrivacyData();
        }
        return status;
    }

    public long getRemainingTime(){
        long currentTimeStamp = DateTime.getDateTime();
        long endTimeStamp = lastPrivacyData.startTimeStamp + lastPrivacyData.duration.getValue();
        Log.d(TAG,"remainging time = "+(endTimeStamp-currentTimeStamp));
        return endTimeStamp-currentTimeStamp;

    }

    private void processPrivacyData(){
        Log.d(TAG, "processPrivacyData()...");
        lastPrivacyData= queryLastPrivacyData();
        Log.d(TAG,"lastPrivacyData="+lastPrivacyData);
        if (lastPrivacyData == null|| !lastPrivacyData.status || getRemainingTime()<=0) {
            Log.d(TAG,"deactivate");
            deactivate();
        }
        else {
            Log.d(TAG,"activate");
            Log.d(TAG,"lastPrivacyData="+lastPrivacyData.startTimeStamp+" "+lastPrivacyData.status+" "+lastPrivacyData.duration.getValue());
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

    public ArrayList<DataType> queryHFlastN(int ds_id, int last_n_sample) {
        return routingManager.queryHFlastN(ds_id, last_n_sample);
    }
    public ArrayList<RowObject> queryLastKey(int ds_id, long last_key, int limit) {
        return routingManager.queryLastKey(ds_id, last_key, limit);
    }

    public ArrayList<RowObject> queryHFLastKey(int ds_id, long last_key, int limit) {
        return routingManager.queryHFLastKey(ds_id, last_key, limit);
    }

    public DataTypeLong querySize() {
        return routingManager.querySize();
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
        Log.d(TAG,"PrivacyManager()..close()..instance="+instance);
        if(instance!=null) {
            listPrivacyListDsId.clear();
            routingManager.close();
            instance = null;
        }
    }

    void activate() {
        handler.removeCallbacks(timer);
        Log.d(TAG, "privacy activated...");
        active=true;
        handler.postDelayed(timer,getRemainingTime());
    }

    public PrivacyData getLastPrivacyData(){
        return lastPrivacyData;
    }

    private PrivacyData queryLastPrivacyData() {
        Gson gson = new Gson();
        dsIdPrivacy = routingManager.register(createDataSourcePrivacy()).getDs_id();

        ArrayList<DataType> dataTypes = routingManager.query(dsIdPrivacy, 1);

        if (dataTypes == null || dataTypes.size() == 0) return null;
        return gson.fromJson((((DataTypeString) dataTypes.get(0))).getSample(), PrivacyData.class);
    }

    void deactivate() {
        Log.d(TAG, "privacy deactivated...");
        listPrivacyListDsId.clear();
        active=false;
        handler.removeCallbacks(timer);
    }
}
