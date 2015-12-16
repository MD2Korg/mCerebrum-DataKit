package org.md2k.datakit.router;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.status.StatusCodes;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
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

public class Publisher {
    private static final String TAG = Publisher.class.getSimpleName();
    int ds_id;
    private List<MessageSubscriber> messageSubscribers;

    private DatabaseSubscriber databaseSubscriber;
    Publisher(int ds_id){
        this.ds_id=ds_id;
        databaseSubscriber=null;
        messageSubscribers =new ArrayList<>();
    }
    public void close(){
        ds_id=-1;
        messageSubscribers.clear();
        messageSubscribers=null;
        databaseSubscriber=null;
    }
    public void setDatabaseSubscriber(DatabaseSubscriber databaseSubscriber){
        this.databaseSubscriber=databaseSubscriber;
    }
    public void receivedData(DataType dataType) {
            notifyAllObservers(dataType);
    }
    boolean isExists(MessageSubscriber subscriber){
        return get(subscriber) != -1;
    }
    int get(MessageSubscriber subscriber){
        for(int i=0;i<messageSubscribers.size();i++)
            if(messageSubscribers.get(i).reply.equals(subscriber.reply))
                return i;
        return -1;
    }

    public int add(MessageSubscriber subscriber){
        if(isExists(subscriber)) return StatusCodes.ALREADY_SUBSCRIBED;
        messageSubscribers.add(subscriber);
        Log.d(TAG,"add()... id="+ds_id+" size="+messageSubscribers.size());
        return StatusCodes.SUCCESS;
    }
    public int remove(MessageSubscriber subscriber){
        if(!isExists(subscriber)) return StatusCodes.DATASOURCE_NOT_EXIST;
        messageSubscribers.remove(get(subscriber));
        return StatusCodes.SUCCESS;
    }
    public void notifyAllObservers(DataType dataType){
        if(messageSubscribers.size()>0)
            Log.d(TAG, "id="+ds_id+" subscriber=" + messageSubscribers.size());
        if(databaseSubscriber!=null) databaseSubscriber.insert(ds_id, dataType);
        for (Iterator<MessageSubscriber> iterator = messageSubscribers.iterator(); iterator.hasNext();) {
            MessageSubscriber subscriber = iterator.next();
            if(!subscriber.update(ds_id,dataType))
                iterator.remove();
        }
    }
}
