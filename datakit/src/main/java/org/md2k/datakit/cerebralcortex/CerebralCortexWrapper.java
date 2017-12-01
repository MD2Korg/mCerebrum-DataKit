package org.md2k.datakit.cerebralcortex;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.datakit.logger.DatabaseLogger;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.mcerebrum.core.access.serverinfo.ServerCP;
import org.md2k.mcerebrum.system.cerebralcortexwebapi.CCWebAPICalls;
import org.md2k.mcerebrum.system.cerebralcortexwebapi.interfaces.CerebralCortexWebApi;
import org.md2k.mcerebrum.system.cerebralcortexwebapi.metadata.MetadataBuilder;
import org.md2k.mcerebrum.system.cerebralcortexwebapi.models.AuthResponse;
import org.md2k.mcerebrum.system.cerebralcortexwebapi.models.stream.DataStream;
import org.md2k.mcerebrum.system.cerebralcortexwebapi.utils.ApiUtils;
import org.md2k.utilities.FileManager;
import org.md2k.utilities.Report.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static android.content.Context.CONNECTIVITY_SERVICE;


/*
 * Copyright (c) 2016, The University of Memphis, MD2K Center
 * - Timothy W. Hnat <twhnat@memphis.edu>
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

public class CerebralCortexWrapper extends Thread {
    private static final String TAG = CerebralCortexWrapper.class.getSimpleName();
    private static String raw_directory = "";
    private static long PRUNE_OFFSET=7*24*60*60*1000;

    private Context context;
    private List<DataSource> restricted;
    private Gson gson = new GsonBuilder().serializeNulls().create();
    private String network_high_freq;
    private String network_low_freq;

    public CerebralCortexWrapper(Context context, List<DataSource> restricted) throws IOException {
        Configuration configuration = ConfigurationManager.getInstance(context).configuration;
        this.context = context;
        this.restricted = restricted;
        this.network_high_freq=configuration.upload.network_high_frequency;
        this.network_low_freq=configuration.upload.network_low_frequency;

        raw_directory = FileManager.getDirectory(context, FileManager.INTERNAL_SDCARD_PREFERRED) + org.md2k.datakit.Constants.RAW_DIRECTORY;
    }

    private void messenger(String message) {
        Intent intent = new Intent(Constants.CEREBRAL_CORTEX_STATUS);
        Time t = new Time(System.currentTimeMillis());
        String msg = t.toString() + ": " + message;
        intent.putExtra("CC_Upload", msg);
        Log.d("CerebralCortexMessenger", msg);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }


    private void publishDataStream(DataSourceClient dsc, CCWebAPICalls ccWebAPICalls, AuthResponse ar, DataStream dsMetadata, DatabaseLogger dbLogger) {
        boolean cont = true;

        int BLOCK_SIZE_LIMIT = Constants.DATA_BLOCK_SIZE_LIMIT;

        long count = 0;

        while (cont) {
            cont = false;

            //Computed Data Store
            List<RowObject> objects;

            objects = dbLogger.queryLastKey(dsc.getDs_id(), Constants.DATA_BLOCK_SIZE_LIMIT);
            count = dbLogger.queryCount(dsc.getDs_id(), true).getSample();


            if (objects.size() > 0) {
                String outputTempFile = FileManager.getDirectory(context, FileManager.INTERNAL_SDCARD_PREFERRED) + "/upload_temp.gz";
                File outputfile = new File(outputTempFile);
                try {
                    FileOutputStream output = new FileOutputStream(outputfile, false);
                    Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), "UTF-8");

                    for (RowObject obj : objects) {
                        writer.write(obj.csvString() + "\n");
                    }
                    writer.close();
                    output.close();
                } catch (IOException e) {
                    Log.e("CerebralCortex", "Compressed file creation failed" + e);
                    e.printStackTrace();
                    return;
                }

                messenger("Offloading data: " + dsc.getDs_id() + "(Remaining: " + count + ")");
                Boolean resultUpload = ccWebAPICalls.putArchiveDataAndMetadata(ar.getAccessToken().toString(), dsMetadata, outputTempFile);
                if (resultUpload) {
                    dbLogger.setSyncedBit(dsc.getDs_id(), objects.get(objects.size() - 1).rowKey);
                    DataTypeLong countRow = dbLogger.queryCount(dsc.getDs_id(), false);
                    if(countRow.getSample()>50000){
                        dbLogger.removeSyncedDataByTime(dsc.getDs_id(), DateTime.getDateTime()-PRUNE_OFFSET);
                    }
                } else {
                    Log.e(TAG, "Error uploading file: " + outputTempFile + " for SQLite database dump");
                    return ;
                }

            }
            if (objects.size() == BLOCK_SIZE_LIMIT) {
                cont = true;
            }

        }

    }


    private void publishDataFiles(DataSourceClient dsc, CCWebAPICalls ccWebAPICalls, AuthResponse ar, DataStream dsMetadata) {
        File directory = new File(raw_directory + "/raw" + dsc.getDs_id());

        FilenameFilter ff = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.contains("_archive") || filename.contains("_corrupt"))
                    return false;
                return true;
            }
        };

        File[] files = directory.listFiles(ff);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHH");

        if (files != null) {
            Arrays.sort(files);
            for (File file : files) {
                Long fileTimestamp = Long.valueOf(file.getName().substring(0, 10));
                Long currentTimestamp = Long.valueOf(dateFormat.format(new Date()));

                if (fileTimestamp < currentTimestamp) {
                    Log.d(TAG, file.getAbsolutePath());

                    Boolean resultUpload = ccWebAPICalls.putArchiveDataAndMetadata(ar.getAccessToken().toString(), dsMetadata, file.getAbsolutePath());
                    if (resultUpload) {
                        File newFile = new File(file.getAbsolutePath().replace(".csv.gz", "_archive.csv.gz"));
                        if (file.renameTo(newFile)) {
                            Log.d(TAG, "Successfully renamed file: " + file.getAbsolutePath());
                        }
                    } else {
                        Log.e(TAG, "Error uploading file: " + file.getName());
                        return;
                    }
                }

            }

        }


    }


    private boolean inRestrictedList(DataSourceClient dsc) {
        for (DataSource d : restricted) {
            if (dsc.getDataSource().getType().equals(d.getType())) {
                return true;
            }
        }
        return false;
    }
/*
    private UserCP readUser(){
        UserCP userInfo=new UserCP();
        UserInfoSelection s= new UserInfoSelection();
        UserInfoCursor c = s.query(context);
        if(c.moveToNext()) {
            userInfo.set(c);
        }
        c.close();
        return userInfo;
    }
    private ConfigCP readConfig(){
        ConfigCP configInfo=new ConfigCP();
        ConfigInfoSelection s= new ConfigInfoSelection();
        ConfigInfoCursor c = s.query(context);
        if(c.moveToNext()) {
            configInfo.set(c);
        }
        c.close();
        return configInfo;
    }
*/

    public void run() {
        if(ServerCP.getServerAddress(context)==null) return;
        Log.w("CerebralCortex", "Starting publishdataKitData");

        DatabaseLogger dbLogger = null;
        if (!DatabaseLogger.isAlive()) {
            Log.w(TAG, "Database is not initialized yet...quitting");
            return;
        }
        try {
            dbLogger = DatabaseLogger.getInstance(context);
            if (dbLogger == null) return;
        } catch (IOException e) {
            return;
        }

        messenger("Starting publish procedure");
        String username= ServerCP.getUserName(context);
        String passwordHash = ServerCP.getPasswordHash(context);
        String token = ServerCP.getToken(context);
        String serverURL=ServerCP.getServerAddress(context);
        if(serverURL==null || serverURL.length()==0 || username==null || username.length()==0 || passwordHash==null || passwordHash.length()==0) {
            messenger("username/password/server address empty");
            return;
        }

        CerebralCortexWebApi ccService = ApiUtils.getCCService(serverURL);
        CCWebAPICalls ccWebAPICalls = new CCWebAPICalls(ccService);

        //TODO: The username needs to be available here for the metadata builder
//        String username = "string";

        //TODO: Either authenticate with Cerebral Cortex service here or pass in the AuthReponse Object
        //TODO: Password needs hashed with SHA256 to obtains string needed
//        AuthResponse ar = ccWebAPICalls.authenticateUser(username, "473287f8298dba7163a897908958f7c0eae733e25d2e027992ea2edc9bed2fa8");
        AuthResponse ar = ccWebAPICalls.authenticateUser(username, passwordHash);


        //TODO: If authentication fails at any point in time, a prompt should be given to reauthenticate

        if (ar != null) {
            messenger("Authenticated with server");
        } else {
            messenger("Authentication Failed");
/*
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("org.md2k.mcerebrum", "org.md2k.mcerebrum.UI.login.ActivityLogin"));
            PendingIntent pendingIntent=PendingIntent.getActivity(context, 0, intent, 0);

            PugNotification.with(context).load().identifier(312).title("Data upload failed").smallIcon(R.mipmap.ic_launcher)
                    .message("Data upload failed. Please login again").autoCancel(true).click(pendingIntent).simple().build();
*/
            return;
        }

        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        List<DataSourceClient> dataSourceClients = dbLogger.find(dataSourceBuilder.build());


        for (DataSourceClient dsc : dataSourceClients) {
            if (!inRestrictedList(dsc)) {
                MetadataBuilder metadataBuilder = new MetadataBuilder();
                DataStream dsMetadata = metadataBuilder.buildDataStreamMetadata(ar.getUserUuid(), dsc);

                if(isNetworkConnectionValid(network_low_freq)) {

                    messenger("Publishing data for " + dsc.getDs_id() + " (" + dsc.getDataSource().getId() + ":" + dsc.getDataSource().getType() + ") to " + dsMetadata.getIdentifier());
                    publishDataStream(dsc, ccWebAPICalls, ar, dsMetadata, dbLogger);
                }
                if(isNetworkConnectionValid(network_high_freq)) {

                    messenger("Publishing raw data for " + dsc.getDs_id() + " (" + dsc.getDataSource().getId() + ":" + dsc.getDataSource().getType() + ") to " + dsMetadata.getIdentifier());
                    publishDataFiles(dsc, ccWebAPICalls, ar, dsMetadata);
                }
            }
        }


        messenger("Upload Complete");
    }
    private boolean isNetworkConnectionValid(String value){
        if(value==null || value.equalsIgnoreCase("ANY")) return true;
        if(value.equalsIgnoreCase("NONE")) return false;
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        if(value.equalsIgnoreCase("WIFI")){
            return manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
                    .isConnectedOrConnecting();
        }
        return manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
                .isConnectedOrConnecting();
    }


}
