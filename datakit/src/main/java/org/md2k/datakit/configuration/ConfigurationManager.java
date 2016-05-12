package org.md2k.datakit.configuration;


import android.content.Context;

import com.google.gson.Gson;

import org.md2k.datakit.Constants;
import org.md2k.datakit.R;
import org.md2k.utilities.FileManager;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

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
public class ConfigurationManager {
    public Configuration configuration;
    private static ConfigurationManager instance=null;
    public static ConfigurationManager getInstance(Context context){
        if(instance==null) instance=new ConfigurationManager(context);
        return instance;
    }
    private ConfigurationManager(Context context) {
        read(context);
    }

    private void read(Context context) {
        try {
            configuration=FileManager.readJSON(Constants.CONFIG_DIRECTORY, Constants.CONFIG_FILENAME, Configuration.class);
        } catch (FileNotFoundException e) {
            readDefault(context);
        }
    }

    void readDefault(Context context) {
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.config)));
        Gson gson = new Gson();
        configuration = gson.fromJson(br, Configuration.class);
    }
    public void write(){
        try {
            FileManager.writeJSON(Constants.CONFIG_DIRECTORY, Constants.CONFIG_FILENAME, configuration);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
