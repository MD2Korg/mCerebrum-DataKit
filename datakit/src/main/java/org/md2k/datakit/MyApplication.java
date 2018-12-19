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

import android.app.Application;
import android.content.Context;

import com.bosphere.filelogger.FL;
import com.bosphere.filelogger.FLConfig;
import com.bosphere.filelogger.FLConst;
import org.md2k.mcerebrum.core.access.MCerebrum;
import org.md2k.utilities.FileManager;

import java.io.File;


/**
 * Starting point for execution.
 */
public class MyApplication extends Application {

    /** Android context. */
    private static Context context;

    /**
     * Calls <code>super.onCreate()</code> and initiates <code>MCerebrum</code> initialization.
     */
    @Override
    public void onCreate() {
        super.onCreate();
        MCerebrum.init(getApplicationContext(), MyMCerebrumInit.class);
        context = this;

        FL.init(new FLConfig.Builder(this)
                .minLevel(FLConst.Level.V)
                .logToFile(true)
                .dir(new File(FileManager.getDirectory(context, FileManager.INTERNAL_SDCARD_PREFERRED) + "logcat"))
                .retentionPolicy(FLConst.DEFAULT_MAX_FILE_COUNT) // ~7 days of restless logging
                .maxFileCount(7) // 7 files
                .maxTotalSize(1024 * 1024) // 1 mb each
                .build());
        FL.setEnabled(true);
    }

    /**
     * Returns the current context.
     *
     * @return The context of this application.
     */
    public static Context getContext(){
        return context;
    }

}

