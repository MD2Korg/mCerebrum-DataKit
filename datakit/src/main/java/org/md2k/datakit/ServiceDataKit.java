/*
 * Copyright (c) 2018, The University of Memphis, MD2K Center of Excellence
 *
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

package org.md2k.datakit;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakit.cerebralcortex.ServiceCerebralCortex;
import org.md2k.datakit.message.MessageController;
import org.md2k.datakitapi.messagehandler.MessageType;
import org.md2k.datakitapi.messagehandler.ResultCallback;
import org.md2k.datakitapi.status.Status;
import org.md2k.utilities.Apps;
import org.md2k.utilities.Report.Log;
import org.md2k.utilities.permission.PermissionInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 */
public class ServiceDataKit extends Service {

    /** Constant used for logging. <p>Uses <code>class.getSimpleName()</code>.</p> */
    private static final String TAG = ServiceDataKit.class.getSimpleName();

    MessageController messageController;
    Messenger mMessenger;
    IncomingHandler incomingHandler;

    /** List of connected data sources. */
    HashMap<String, Messenger> connectedList;

    /** Set of message handlers. */
    HashSet<Messenger> messengers;

    /**
     * Upon creation, <code>DataKit</code> registers a message receiver with an intent filter.
     *
     * <p>
     *     Intents with the <code>"datakit"</code> action are filtered out.
     * </p>
     */
    @Override
    public void onCreate() {
        super.onCreate();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("datakit"));
        PermissionInfo permissionInfo = new PermissionInfo();
        permissionInfo.getPermissions(this, new ResultCallback<Boolean>() {
            /**
             * If permissions are granted, the service starts.
             *
             * @param result Result of the callback from <code>.getPermissions()</code>.
             */
            @Override
            public void onResult(Boolean result) {
                if(result)
                    start();
                else
                    stopSelf();
            }
        });
    }

    /**
     * Receives start and stop messages.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        /**
         * Starts or stops the service accordingly.
         *
         * @param context Android context
         * @param intent Android intent
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String op = intent.getStringExtra("action");
            if ("start".equals(op))
                start();
            else if ("stop".equals(op))
                stop();
        }
    };

    /**
     * Logs the stop command, stops the service, and nullifies <code>messageController</code> and
     * <code>incomingHandler</code>.
     */
    void stop() {
        Log.d(TAG, "stop()...");
        if(Apps.isServiceRunning(this,"org.md2k.datakit.cerebralcortex.ServiceCerebralCortex")){
            Intent intent = new Intent(this, ServiceCerebralCortex.class);
            stopService(intent);
        }
        if (messageController != null) {
            messageController.close();
            messageController = null;
        }
        incomingHandler = null;
        disconnectAll();
    }

    /**
     * 
     */
    void disconnectAll() {
        Messenger replyTo;
        Message outgoingMessage;
        Bundle bundle = new Bundle();
        bundle.putParcelable(Status.class.getSimpleName(), new Status(Status.INTERNAL_ERROR));
        outgoingMessage = prepareMessage(bundle, MessageType.INTERNAL_ERROR);

        for (String name : connectedList.keySet()) {
            replyTo = connectedList.get(name);
            try {
                replyTo.send(outgoingMessage);
            } catch (RemoteException ignored) {
            }
        }
        connectedList.clear();
        messengers.clear();
    }

    void start() {
        incomingHandler = new IncomingHandler();
        connectedList = new HashMap<>();
        messengers = new HashSet<>();
        Log.d(TAG, "start()...");
        try {
            messageController = MessageController.getInstance(getApplicationContext());
            mMessenger = new Messenger(incomingHandler);
        } catch (IOException ignored) {
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG,"onUnbind()...");
        String pName = intent.getStringExtra("name");
        Messenger messenger = intent.getParcelableExtra("messenger");
        connectedList.remove(pName);
        messengers.remove(messenger);
        Log.d(TAG, "name=" + pName + " messenger=" + messenger);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()...");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        messengers = null;
        connectedList = null;
        if (messageController != null) {
            messageController.close();
            messageController = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG,"onBind()...");
        String pName = intent.getStringExtra("name");
        Messenger messenger = intent.getParcelableExtra("messenger");
        Log.d(TAG, "name=" + pName + " messenger=" + messenger);
        connectedList.put(pName, messenger);
        messengers.add(messenger);
        return mMessenger.getBinder();
    }

    public Message prepareMessage(Bundle bundle, int messageType) {
        Message message = Message.obtain(null, 0, 0, 0);
        message.what = messageType;
        message.setData(bundle);
        return message;
    }

    private class IncomingHandler extends Handler {
        Messenger replyTo;

        @Override
        public void handleMessage(Message incomingMessage) {
            Message outgoingMessage;
            if (messageController == null) {
                Log.d(TAG, "error...messageController=null");
                Bundle bundle = new Bundle();
                bundle.putParcelable(Status.class.getSimpleName(), new Status(Status.INTERNAL_ERROR));
                outgoingMessage = prepareMessage(bundle, incomingMessage.what);
            } else
                outgoingMessage = messageController.execute(incomingMessage);
            if (outgoingMessage != null) {
                replyTo = incomingMessage.replyTo;
                try {
                    replyTo.send(outgoingMessage);
                } catch (RemoteException ignored) {
                }
            }
        }
    }
}
