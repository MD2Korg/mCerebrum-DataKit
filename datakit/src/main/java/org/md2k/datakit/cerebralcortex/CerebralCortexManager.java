package org.md2k.datakit.cerebralcortex;

import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;

import org.md2k.datakit.R;
import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.utilities.Apps;
import org.md2k.utilities.UI.AlertDialogs;

import java.io.IOException;


/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Timothy Hnat <twhnat@memphis.edu>
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
public class CerebralCortexManager {
    private static CerebralCortexManager instance;
    private static CerebralCortexWrapper task;
    private Context context;
    private boolean active;
    private Configuration configuration;
    private Handler handler;
    Runnable publishData = new Runnable() {
        @Override
        public void run() {

            if (task != null && task.lastUpload < (System.currentTimeMillis() - 360000)) {
                task.cancel(true);
            }
            if (task != null && task.getStatus() == AsyncTask.Status.RUNNING) {
                handler.removeCallbacks(publishData);
            } else {
                try {
                    long time = Apps.serviceRunningTime(context.getApplicationContext(), org.md2k.datakit.Constants.SERVICE_NAME);
                    if (time > 0) {
                        task = new CerebralCortexWrapper(context, configuration.upload.url, configuration.upload.restricted_datasource);
                        task.execute();
                    }
                } catch (IOException e) {
                    AlertDialogs.AlertDialog(context, "Error", e.getMessage(), R.drawable.ic_error_red_50dp, "Ok", null, null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                }
            }
            handler.postDelayed(publishData, configuration.upload.interval);
        }
    };

    CerebralCortexManager(Context context) {
        this.context = context;
        handler = new Handler();
        active = false;
        configuration=ConfigurationManager.getInstance(context).configuration;
    }

    public static CerebralCortexManager getInstance(Context context) throws IOException {
        if (instance == null)
            instance = new CerebralCortexManager(context);
        return instance;
    }

    void start() {
        active = true;
        if(configuration.upload.enabled)
            handler.post(publishData);
    }

    public boolean isActive() {
        return active;
    }

    void stop() {
        active = false;
        if(task!=null)
        task.cancel(true);
        handler.removeCallbacks(publishData);
    }

    public boolean isAvailable() {
        return (configuration != null);
    }
}
