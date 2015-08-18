package org.md2k.datakit.datarouter;

import android.os.Messenger;
import android.util.SparseArray;
import org.md2k.datakit.Logger.DatabaseLogger;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.status.Status;
import org.md2k.datakitapi.status.StatusCodes;

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
public class Publishers {
    private static final String TAG = Publishers.class.getSimpleName();
    SparseArray<Publisher> publishers;
    private static Publishers instance=null;
    public static Publishers getInstance(){
        if(instance==null)
            instance=new Publishers();
        return instance;
    }
    Publishers(){
        publishers=new SparseArray<>();
    }
    public void add(DataSourceClient dataSourceClient){
        if(dataSourceClient.getDs_id()==-1) return;
        if(publishers.indexOfKey(dataSourceClient.getDs_id())<0) {
            publishers.put(dataSourceClient.getDs_id(), new Publisher(dataSourceClient.getDs_id()));
        }
    }
    public Status remove(int ds_id){
        if(publishers.indexOfKey(ds_id)<0) return new Status(StatusCodes.DATASOURCE_NOT_FOUND);
        publishers.remove(ds_id);
        return new Status(StatusCodes.SUCCESS);
    }

    public Status subscribe(int ds_id,Messenger reply){
        if(publishers.indexOfKey(ds_id)<0)
            return new Status(StatusCodes.DATASOURCE_NOT_FOUND);
        Publisher publisher=publishers.get(ds_id);
        return publisher.add(new MessageSubscriber(reply));
    }
    public Status subscribe(int ds_id,DatabaseLogger databaseLogger){
        if(publishers.indexOfKey(ds_id)<0)
            return new Status(StatusCodes.DATASOURCE_NOT_FOUND);
        Publisher publisher=publishers.get(ds_id);
        return publisher.add(new DatabaseSubscriber(databaseLogger));
    }

    public Status unsubscribe(int ds_id,Messenger reply){
        if(publishers.indexOfKey(ds_id)<0)
            return new Status(StatusCodes.DATASOURCE_NOT_FOUND);
        Publisher publisher=publishers.get(ds_id);
        return publisher.remove(new MessageSubscriber(reply));
    }

}
