package org.md2k.datakit;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import org.md2k.datakit.Logger.DatabaseLogger;
import org.md2k.datakit.manager.DataManager;
import org.md2k.datakit.manager.DataSourceManager;
import org.md2k.datakit.manager.Manager;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.messagehandler.MessageType;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.utilities.Report.Log;

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

public class ServiceDataKit extends Service {
    private static final String TAG = ServiceDataKit.class.getSimpleName();
    DatabaseLogger databaseLogger = null;
    Messenger mMessenger;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()...");
        databaseLogger = DatabaseLogger.getInstance(ServiceDataKit.this);
        mMessenger = new Messenger(new IncomingHandler());
        Log.d(TAG, "databaseLogger=" + databaseLogger);
        Log.d(TAG, "...onCreate()");
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()...");
        if (databaseLogger != null) {
            databaseLogger.close();
            databaseLogger=null;
        }
        super.onDestroy();
        Log.d(TAG, "...onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mMessenger.getBinder();
    }

    private class IncomingHandler extends Handler {
        DataSourceManager dataSourceManager;
        DataManager dataManager;
        Message message;
        Messenger replyTo;

        IncomingHandler() {
            if (databaseLogger != null) {
                dataSourceManager = new DataSourceManager();
                dataManager = new DataManager();
            }
        }


        @Override
        public void handleMessage(Message msg) {
            message = null;
            try {
                if (databaseLogger == null) {
                    Log.d(TAG,"handleMessage(): databaseLogger=null msg="+msg.what);
                    message = Manager.prepareErrorMessage(msg.what);
                } else {
                    switch (msg.what) {
                        case MessageType.REGISTER:
                            Log.d(TAG, "IncomingHandler -> handleMessge() -> Register...");
                            message = dataSourceManager.register((DataSource) msg.getData().getSerializable(DataSource.class.getSimpleName()));
                            Log.d(TAG, "IncomingHandler -> handleMessge() -> ...Register");
                            break;
                        case MessageType.UNREGISTER:
                            message = dataSourceManager.unregister(msg.getData().getInt("ds_id"));
                            break;
                        case MessageType.FIND:
                            message = dataSourceManager.find((DataSource) msg.getData().getSerializable(DataSource.class.getSimpleName()));
                            break;
                        case MessageType.INSERT:

                            message = dataManager.insert(msg.getData().getInt("ds_id"), (DataType) msg.getData().getSerializable(DataType.class.getSimpleName()));
                            break;
                        case MessageType.QUERY:
                            message = dataManager.query(msg.getData().getInt("ds_id"), msg.getData().getLong("starttimestamp"), msg.getData().getLong("endtimestamp"));
                            break;
                        case MessageType.SUBSCRIBE:
                            Log.d(TAG,"subscribe");
                            message = dataSourceManager.subscribe(msg.getData().getInt("ds_id"), msg.replyTo);
                            break;
                        case MessageType.UNSUBSCRIBE:
                            message = dataSourceManager.unsubscribe(msg.getData().getInt("ds_id"), msg.replyTo);
                            break;
                    }
                }
                if (message != null) {
                    replyTo = msg.replyTo;
                    replyTo.send(message);
                }

            } catch (RemoteException rme) {
                //Show an Error Message
                Log.d(TAG, "Error: invocation failed");
            }
        }
    }
}
