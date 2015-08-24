package org.md2k.datakit.datarouter;

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

import org.md2k.datakit.Logger.DatabaseLogger;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.status.Status;
import org.md2k.datakitapi.status.StatusCodes;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;
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
    Publisher(int ds_id, DatabaseLogger databaseLogger){
        this.ds_id=ds_id;
        databaseSubscriber=new DatabaseSubscriber(databaseLogger);
        messageSubscribers =new ArrayList<>();
    }
    public void receivedData(DataType dataType) {
        notifyAllObservers(dataType);
    }
    boolean isExists(MessageSubscriber subscriber){
        for(MessageSubscriber subscriber1: messageSubscribers)
            if(subscriber1.reply.equals(subscriber.reply))
                return true;
        return false;
    }
    public Status add(MessageSubscriber subscriber){
        Log.d(TAG,"Publisher->add()");
        if(isExists(subscriber)) return new Status(StatusCodes.ALREADY_SUBSCRIBED);
        messageSubscribers.add(subscriber);
        return new Status(StatusCodes.SUCCESS);
    }
    public Status remove(MessageSubscriber subscriber){
        if(!isExists(subscriber)) return new Status(StatusCodes.DATASOURCE_NOT_FOUND);
        messageSubscribers.remove(subscriber);
        return new Status(StatusCodes.SUCCESS);
    }
    public void notifyAllObservers(DataType dataType){
        Log.d(TAG, "Publisher->notifyAllObservers() ds_id="+ds_id);
        if(databaseSubscriber!=null) databaseSubscriber.update(ds_id,dataType);
        for (MessageSubscriber subscriber : messageSubscribers) {
            subscriber.update(ds_id,dataType);
        }
    }
}
