package org.md2k.datakit.cerebralcortex;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;

import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.mcerebrum.core.access.appinfo.AppInfo;
import org.md2k.mcerebrum.core.access.serverinfo.ServerCP;
import org.md2k.utilities.Apps;

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
    private Runnable publishData = new Runnable() {
        @Override
        public void run() {
            if(ServerCP.getServerAddress(context)==null) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("SERVER_ERROR"));
                stop();
                return;
            }
            if (task != null) {
                handler.removeCallbacks(publishData);
            }
            try {
                task = new CerebralCortexWrapper(context, configuration.upload.restricted_datasource);
                task.setPriority(Thread.MIN_PRIORITY);
                long time = AppInfo.serviceRunningTime(context.getApplicationContext(), org.md2k.datakit.Constants.SERVICE_NAME);
                if (time > 0) { //TWH: TEMPORARY
                    task.start();
                }
            } catch (IOException e) {
/*
                AlertDialogs.AlertDialog(context, "Error", e.getMessage(), R.drawable.ic_error_red_50dp, "Ok", null, null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
*/
            }


            handler.postDelayed(publishData, configuration.upload.interval);
        }
    };

    private CerebralCortexManager(Context context) {
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
        if (configuration.upload.enabled) {
            active = true;
            handler.post(publishData);
        }
    }

    boolean isActive() {
        return active;
    }

    void stop() {
        active = false;
        handler.removeCallbacks(publishData);
    }

    boolean isAvailable() {
        return (configuration != null);
    }
}
