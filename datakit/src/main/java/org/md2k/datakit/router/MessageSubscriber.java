package org.md2k.datakit.router;

import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.messagehandler.MessageType;

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
public class MessageSubscriber{
    private static final String TAG = MessageSubscriber.class.getSimpleName();
    Messenger reply;
    String packageName;
    public MessageSubscriber(String packageName, Messenger reply){
        this.packageName=packageName;
        this.reply=reply;
    }
    public boolean update(int ds_id,DataType data) {
        Bundle bundle=new Bundle();
        bundle.putParcelable(DataType.class.getSimpleName(), data);
        bundle.putInt("ds_id",ds_id);
        Message message=prepareMessage(bundle, MessageType.SUBSCRIBED_DATA);
        try {
            reply.send(message);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }
    public Message prepareMessage(Bundle bundle, int messageType) {
        Message message = Message.obtain(null, 0, 0, 0);
        message.what = messageType;
        message.setData(bundle);
        return message;
    }
}
