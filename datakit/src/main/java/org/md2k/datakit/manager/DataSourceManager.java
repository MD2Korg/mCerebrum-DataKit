package org.md2k.datakit.manager;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;

import org.md2k.datakit.datarouter.Publishers;
import org.md2k.datakitapi.messagehandler.MessageType;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.status.Status;
import org.md2k.datakitapi.status.StatusCodes;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
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

public class DataSourceManager extends Manager{
    private static final String TAG = DataSourceManager.class.getSimpleName();
    Publishers publishers;

    public DataSourceManager() {
        super();
        publishers=Publishers.getInstance();
    }
    public void close(){
        publishers.close();
    }


    public Message register(DataSource dataSource) {
        Log.d(TAG,"register: "+dataSource.getType());
        DataSourceClient dataSourceClient = registerDataSource(dataSource);
        if(dataSource.isPersistent()) {
            publishers.addPublisher(dataSourceClient.getDs_id(), databaseLogger);
        }
        else {
            publishers.addPublisher(dataSourceClient.getDs_id());
            publishers.setActive(dataSourceClient.getDs_id(),true);
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(DataSourceClient.class.getSimpleName(), dataSourceClient);
        return prepareMessage(bundle, MessageType.REGISTER);
    }

    public Message unregister(int ds_id) {
        int statusCode=publishers.remove(ds_id);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Status.class.getSimpleName(), new Status(statusCode));
        return prepareMessage(bundle, MessageType.UNREGISTER);
    }

    public Message subscribe(int ds_id, Messenger reply) {
        int statusCode = publishers.subscribe(ds_id, reply);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Status.class.getSimpleName(), new Status(statusCode));
        return prepareMessage(bundle, MessageType.SUBSCRIBE);
    }

    public Message unsubscribe(int ds_id, Messenger reply) {
        int statusCode = publishers.unsubscribe(ds_id, reply);
        Bundle bundle = new Bundle();
        bundle.putSerializable(Status.class.getSimpleName(), new Status(statusCode));
        return prepareMessage(bundle, MessageType.UNSUBSCRIBE);
    }

    public Message find(DataSource dataSource) {
        ArrayList<DataSourceClient> dataSourceClients = databaseLogger.find(dataSource);
        if(dataSourceClients.size()>0){
            for(int i=0;i<dataSourceClients.size();i++){
                if(publishers.isExist(dataSourceClients.get(i).getDs_id())) {
                    int ds_id=dataSourceClients.get(i).getDs_id();
                    DataSourceClient dataSourceClient = new DataSourceClient(ds_id,dataSource,new Status(StatusCodes.DATASOURCE_ACTIVE));
                    dataSourceClients.set(i,dataSourceClient);
                }
            }
        }
        Bundle bundle = new Bundle();
        bundle.putSerializable(DataSourceClient.class.getSimpleName(), dataSourceClients);
        return prepareMessage(bundle, MessageType.FIND);
    }

    public DataSourceClient registerDataSource(DataSource dataSource) {
        DataSourceClient dataSourceClient;
        if (dataSource == null || dataSource.getType()==null || dataSource.getApplication().getType()==null)
            dataSourceClient = new DataSourceClient(-1, dataSource, new Status(StatusCodes.DATASOURCE_INVALID));
        else {
            ArrayList<DataSourceClient> dataSourceClients = databaseLogger.find(dataSource);
            if (dataSourceClients.size() == 0) {
                dataSourceClient = databaseLogger.register(dataSource);
            } else if (dataSourceClients.size() == 1) {
                dataSourceClient = new DataSourceClient(dataSourceClients.get(0).getDs_id(), dataSourceClients.get(0).getDataSource(), new Status(StatusCodes.DATASOURCE_EXIST));
            } else {
                dataSourceClient = new DataSourceClient(-1, dataSource, new Status(StatusCodes.DATASOURCE_MULTIPLE_EXIST));
            }
        }
        return dataSourceClient;
    }
}
