package org.md2k.datakit;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.WindowManager;
import org.md2k.datakit.message.MessageController;
import org.md2k.utilities.Report.Log;

import java.io.IOException;

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

public class ServiceDataKit extends Service {
    private static final String TAG = ServiceDataKit.class.getSimpleName();
    MessageController messageController;
    Messenger mMessenger;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate()...");
        try {
            messageController = MessageController.getInstance(ServiceDataKit.this);
        } catch (IOException e) {
            showAlertDialog(this,e.getMessage());
            e.printStackTrace();
        }
        mMessenger = new Messenger(new IncomingHandler());
        Log.d(TAG, "...onCreate()");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "unbind()...package=" + intent.getPackage());
        return super.onUnbind(intent);
    }

    static void showAlertDialog(final Context context, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle("Error")
                .setIcon(R.drawable.ic_error_red_50dp)
                .setMessage(message)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .create();
        alertDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        alertDialog.show();
    }

    @Override
    public void onDestroy() {
        messageController.close();
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mMessenger.getBinder();
    }

    private class IncomingHandler extends Handler {
        Messenger replyTo;

        IncomingHandler() {
        }


        @Override
        public void handleMessage(Message incomingMessage) {
            Message outgoingMessage = messageController.execute(incomingMessage);
            if (outgoingMessage != null) {
                replyTo = incomingMessage.replyTo;
                try {
                    replyTo.send(outgoingMessage);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
