package org.md2k.datakit.cerebralcortex;

import android.content.Context;

import org.md2k.datakit.cerebralcortex.config.ConfigManager;
import org.md2k.utilities.Report.Log;

import java.io.IOException;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * - Timothy Hnat <twhnat@memphis.edu>
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
public class CerebralCortexController {
    private static final String TAG = CerebralCortexController.class.getSimpleName();
    private static CerebralCortexController instance = null;
    Context context;

    CerebralCortexManager cerebralCortexManager;
    ConfigManager configManager;

    CerebralCortexController(Context context) throws IOException {
        Log.d(TAG, "CerebralCortexController()...constructor()...");
        this.context = context;
        cerebralCortexManager = CerebralCortexManager.getInstance(context);
        configManager = ConfigManager.getInstance(context);
    }

    public static CerebralCortexController getInstance(Context context) throws IOException {
        if (instance == null) instance = new CerebralCortexController(context);
        return instance;
    }

    public boolean isAvailable() {
        return configManager.isAvailable();
    }

    public boolean isActive() {
        return cerebralCortexManager.isActive();
    }

}
