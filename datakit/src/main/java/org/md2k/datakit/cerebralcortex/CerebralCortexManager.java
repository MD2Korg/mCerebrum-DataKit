package org.md2k.datakit.cerebralcortex;

import android.content.Context;
import android.os.Handler;

import org.md2k.datakit.cerebralcortex.config.Config;
import org.md2k.datakitapi.DataKitAPI;

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
    private Context context;
    private DataKitAPI dataKitAPI;
    private boolean active;
    private Config config;
    private Handler handler;
    private CerebralCortexWrapper task;
    Runnable publishData = new Runnable() {
        @Override
        public void run() {
            task.execute();
            handler.postDelayed(publishData, config.getUpload_interval());
        }
    };

    CerebralCortexManager(Context context, Config config) {
        this.context = context;
        this.config = config;
        dataKitAPI = DataKitAPI.getInstance(context);
        handler = new Handler();
        active = false;
    }

    public static CerebralCortexManager getInstance(Context context) throws IOException {
        CerebralCortexManager instance;
        if (instance == null)
            instance = new CerebralCortexManager(context);
        return instance;
    }

    void start() {
        active = true;
        task = new CerebralCortexWrapper(context, dataKitAPI, config.getUrl(), config.getRestricted_datasource());
        handler.post(publishData);
    }

    public boolean isActive() {
        return active;
    }

    void stop() {
        active = false;
        handler.removeCallbacks(publishData);
    }
}
