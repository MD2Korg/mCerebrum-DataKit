package org.md2k.datakit.router;

import android.content.Context;
import android.os.Messenger;

import org.md2k.datakit.logger.DatabaseLogger;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.status.Status;
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


public class RoutingManager {
    private static final String TAG = RoutingManager.class.getSimpleName();
    private static RoutingManager instance;
    Context context;
    DatabaseLogger databaseLogger;
    Publishers publishers;

    private RoutingManager(Context context) throws IOException {
        Log.d(TAG, "RoutingManager()....constructor()");
        this.context=context;
        databaseLogger=DatabaseLogger.getInstance(context);
        publishers=new Publishers();
    }

    public static RoutingManager getInstance(Context context) throws IOException {
        if (instance == null)
            instance = new RoutingManager(context);
        return instance;
    }

    public DataSourceClient register(DataSource dataSource) {
        DataSourceClient dataSourceClient = registerDataSource(dataSource);
        if(dataSource.isPersistent()) {
            publishers.addPublisher(dataSourceClient.getDs_id(), databaseLogger);
        }
        else {
            publishers.addPublisher(dataSourceClient.getDs_id());
        }
        return dataSourceClient;
    }
    public Status insert(int ds_id, DataType dataType){
        return publishers.receivedData(ds_id, dataType);
    }

    public Status insertHF(int ds_id, DataTypeDoubleArray dataType) {
        return publishers.receivedDataHF(ds_id, dataType);
    }
    public ArrayList<DataType> query(int ds_id,long starttimestamp, long endtimestamp){
        return databaseLogger.query(ds_id, starttimestamp, endtimestamp);
    }
    public ArrayList<DataType> query(int ds_id,int last_n_sample){
        return databaseLogger.query(ds_id, last_n_sample);
    }
    public ArrayList<RowObject> queryLastKey(int ds_id,long last_key, int limit){
        return databaseLogger.queryLastKey(ds_id, last_key, limit);
    }

    public ArrayList<RowObject> queryHFLastKey(int ds_id, long last_key, int limit) {
        return databaseLogger.queryHFLastKey(ds_id, last_key, limit);
    }

    public DataTypeLong querySize() {
        return databaseLogger.querySize();
    }

    public Status unregister(int ds_id) {
        int statusCode=publishers.remove(ds_id);
        return new Status(statusCode);
    }

    public Status subscribe(int ds_id, Messenger reply) {
        int statusCode = publishers.subscribe(ds_id, reply);
        return  new Status(statusCode);
    }

    public Status unsubscribe(int ds_id, Messenger reply) {
        int statusCode = publishers.unsubscribe(ds_id, reply);
        return new Status(statusCode);
    }

    public ArrayList<DataSourceClient> find(DataSource dataSource) {
        ArrayList<DataSourceClient> dataSourceClients = databaseLogger.find(dataSource);
        if(dataSourceClients.size()>0){
            for(int i=0;i<dataSourceClients.size();i++){
                if(publishers.isExist(dataSourceClients.get(i).getDs_id())) {
                    int ds_id=dataSourceClients.get(i).getDs_id();
                    DataSourceClient dataSourceClient = new DataSourceClient(ds_id,dataSourceClients.get(i).getDataSource(),new Status(Status.DATASOURCE_ACTIVE));
                    dataSourceClients.set(i,dataSourceClient);
                }
            }
        }
        return dataSourceClients;
    }

    private DataSourceClient registerDataSource(DataSource dataSource) {
        DataSourceClient dataSourceClient;
        if (dataSource == null || dataSource.getType()==null || dataSource.getApplication().getId()==null)
            dataSourceClient = new DataSourceClient(-1, dataSource, new Status(Status.DATASOURCE_INVALID));
        else {
            ArrayList<DataSourceClient> dataSourceClients = databaseLogger.find(dataSource);
            if (dataSourceClients.size() == 0) {
                dataSourceClient = databaseLogger.register(dataSource);
            } else if (dataSourceClients.size() == 1) {
                dataSourceClient = new DataSourceClient(dataSourceClients.get(0).getDs_id(), dataSourceClients.get(0).getDataSource(), new Status(Status.DATASOURCE_EXIST));
            } else {
                dataSourceClient = new DataSourceClient(-1, dataSource, new Status(Status.DATASOURCE_MULTIPLE_EXIST));
            }
        }
        return dataSourceClient;
    }
    public void close(){
        Log.d(TAG,"RoutingManager()...close()...");
        if(instance!=null) {
            publishers.close();
            databaseLogger.close();
            instance = null;
        }
    }
}
