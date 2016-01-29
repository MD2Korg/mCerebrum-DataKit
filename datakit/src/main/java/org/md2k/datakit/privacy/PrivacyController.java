package org.md2k.datakit.privacy;

import android.content.Context;

import com.google.gson.Gson;

import org.md2k.datakitapi.datatype.DataTypeString;
import org.md2k.datakitapi.time.DateTime;
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
public class PrivacyController {
    private static final String TAG = PrivacyController.class.getSimpleName();
    private static PrivacyController instance=null;
    Context context;
    PrivacyManager privacyManager;
    PrivacyConfiguration privacyConfiguration;
    public static PrivacyController getInstance(Context context) throws IOException {
        if(instance==null) instance=new PrivacyController(context);
        return instance;
    }
    PrivacyController(Context context) throws IOException {
        Log.d(TAG,"PrivacyController()...constructor()...");
        this.context=context;
        privacyManager=PrivacyManager.getInstance(context);
        privacyConfiguration = PrivacyConfiguration.getInstance(context);
    }
    public boolean isPrivacyTypeExists(String id){
        PrivacyData privacyData=privacyManager.getLastPrivacyData();
        if(privacyData==null) return false;
        for(int i=0;i<privacyData.getPrivacyTypes().size();i++)
            if(privacyData.getPrivacyTypes().get(i).getId().equals(id)) return true;
        return false;
    }
    public PrivacyConfiguration getPrivacyConfiguration(){
        return privacyConfiguration;
    }
    public boolean isAvailable(){
        return privacyConfiguration.isAvailable();
    }
    public boolean isActive(){
        return privacyManager.isActive();
    }
    public long getRemainingTime() {
        return privacyManager.getRemainingTime();
    }
    public PrivacyData getPrivacyData(){
        return privacyManager.getLastPrivacyData();
    }
    public String getActiveList(){
        String list="";
        if(!isActive()) return null;
        PrivacyData privacyData = privacyManager.getLastPrivacyData();
        for(int i=0;i<privacyData.privacyTypes.size();i++){
            if(i!=0) list+=",";
            list+=privacyData.privacyTypes.get(i).getTitle();
        }
        return list;
    }
    public void insertPrivacyData(PrivacyData privacyData){
        Gson gson=new Gson();
        String str=gson.toJson(privacyData);
        Log.d(TAG, "dsid="+privacyManager.dsIdPrivacy+" privacydata=" + str);
        DataTypeString dataTypeString=new DataTypeString(DateTime.getDateTime(),str);
        privacyManager.insert(privacyManager.dsIdPrivacy, dataTypeString);
    }
}
