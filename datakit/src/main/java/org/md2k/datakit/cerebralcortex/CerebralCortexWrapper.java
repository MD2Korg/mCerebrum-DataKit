package org.md2k.datakit.cerebralcortex;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.bluelinelabs.logansquare.LoganSquare;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.md2k.datakit.cerebralcortex.communication.CerebralCortexData;
import org.md2k.datakit.cerebralcortex.communication.CerebralCortexDataResponse;
import org.md2k.datakit.cerebralcortex.communication.CerebralCortexDataSource;
import org.md2k.datakit.cerebralcortex.communication.CerebralCortexDataSourceResponse;
import org.md2k.datakit.cerebralcortex.communication.ParticipantRegistration;
import org.md2k.datakit.cerebralcortex.communication.StudyInfo;
import org.md2k.datakit.cerebralcortex.communication.StudyInfoCC;
import org.md2k.datakit.cerebralcortex.communication.StudyInfoCCResponse;
import org.md2k.datakit.cerebralcortex.communication.UserInfo;
import org.md2k.datakit.cerebralcortex.communication.UserInfoCC;
import org.md2k.datakit.cerebralcortex.communication.UserInfoCCResponse;
import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.datakit.logger.DatabaseLogger;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.utilities.FileManager;
import org.md2k.utilities.Report.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Time;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


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
    public static String CCDIR = "";
    private static String raw_directory = "";
    public long lastUpload;

    OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    OkHttpClient gzipClient = new OkHttpClient.Builder()
            .addInterceptor(new GzipRequestInterceptor())
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();


    private Context context;
    private String requestURL;
    private List<DataSource> restricted;
    private Gson gson = new GsonBuilder().serializeNulls().create();

    public CerebralCortexWrapper(Context context, String url, List<DataSource> restricted) throws IOException {
        Configuration configuration = ConfigurationManager.getInstance(context).configuration;
        this.context = context;
        this.requestURL = url;
        this.restricted = restricted;
        raw_directory = FileManager.getDirectory(context, configuration.database.location) + org.md2k.datakit.Constants.RAW_DIRECTORY;
        if (configuration.archive.enabled) {
            CCDIR = FileManager.getDirectory(context, configuration.archive.location) + org.md2k.datakit.Constants.ARCHIVE_DIRECTORY;
        } else CCDIR = null;
    }

    private void messenger(String message) {
        Intent intent = new Intent(Constants.CEREBRAL_CORTEX_STATUS);
        Time t = new Time(System.currentTimeMillis());
        String msg = t.toString() + ": " + message;
        intent.putExtra("CC_Upload", msg);
        Log.d("CerebralCortexMessenger", msg);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }

    private void archiveJsonData(String data, int ds_id, String filename) {
        if (CCDIR == null) return;
        File outputDir = new File(CCDIR + "ds" + ds_id + "/");
        outputDir.mkdirs();

        File outputfile = new File(outputDir + "/" + filename);
        if (!outputfile.exists()) {
            try {
                FileOutputStream output = new FileOutputStream(outputfile, false);
                Writer writer = new OutputStreamWriter(new GZIPOutputStream(output), "UTF-8");

                writer.write(data);

                writer.close();
                output.close();
            } catch (IOException e) {
                Log.e("CerebralCortex", "JSON Archive failed" + e);
                e.printStackTrace();
            }
        } else {
            //File exists, skipping
        }
    }

    private void publishDataStream(DataSourceClient dsc, CerebralCortexDataSourceResponse ccdpResponse, DatabaseLogger dbLogger) {
        String APIendpoint;
        String dataResult = null;
        boolean cont = true;

        while (cont) {
            cont = false;

            CerebralCortexData ccdata = new CerebralCortexData(ccdpResponse.datastream_id);

            //Computed Data Store
            List<RowObject> objects;
            int BLOCK_SIZE_LIMIT;
            long count = 0L;

            APIendpoint = "datapoints/bulkload";
            objects = dbLogger.queryLastKey(dsc.getDs_id(), Constants.DATA_BLOCK_SIZE_LIMIT);
            count = dbLogger.queryCount(dsc.getDs_id(), true).getSample();
            BLOCK_SIZE_LIMIT = Constants.DATA_BLOCK_SIZE_LIMIT;


            if (objects.size() > 0) {
                messenger("Offloading data: " + dsc.getDs_id() + "(Remaining: " + count + ")");

                long key = objects.get(objects.size() - 1).rowKey;
                for (RowObject obj : objects) {
                    ccdata.data.add(obj.toArrayForm());
                }

                try {
                    messenger("JSON upload started: " + dsc.getDs_id() + "(Size: " + ccdata.data.size() + ")");
                    String data = new GsonBuilder().setPrettyPrinting().create().toJson(ccdata);
                    String filename = dsc.getDs_id() + "_" + objects.get(0).data.getDateTime() + ".json.gz";
                    archiveJsonData(data, dsc.getDs_id(), filename);

                    dataResult = cerebralCortexAPI(requestURL + APIendpoint, data);

                    if (dataResult == null) {
                        break;
                    }
                    CerebralCortexDataResponse ccdr = LoganSquare.parse(dataResult, CerebralCortexDataResponse.class);
                    if (ccdr.count > 0) {
                        messenger("Uploaded " + dsc.getDs_id() + "(Size: " + ccdr.count + ")");
                        dbLogger.setSyncedBit(dsc.getDs_id(), key);
                        lastUpload = System.currentTimeMillis();
                        messenger("CloudSync " + dsc.getDs_id() + "(Remaining: " + (count - ccdr.count) + ")");
                    }

                    if (ccdr.count == BLOCK_SIZE_LIMIT) {
                        cont = true;
                    }
                } catch (IOException e) {
                    Log.e("CerebralCortex", "Bulk load error: " + e);
                    messenger("Bulk load error");
                    break;
                }

            }
        }
    }


    private void publishDataFiles(DataSourceClient dsc, CerebralCortexDataSourceResponse ccdpResponse) {
        final String APIendpoint = "rawdatapoints/bulkload";

        CerebralCortexData ccdata = new CerebralCortexData(ccdpResponse.datastream_id);

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

        if (files != null) {
            Arrays.sort(files);
            if (files.length > 1) { //Check for multiple files.  The last file is the current one and should not be moved/copied
                for (int i = 0; i < files.length - 1; i++) {
                    Log.d(TAG, files[i].getAbsolutePath());
                    String dataResult = null;
                    try {
                        dataResult = cerebralCortexAPI_RAWFile(requestURL + APIendpoint, new Long(ccdata.datastream_id).intValue(), files[i]);
                        if (dataResult == null) {
                            Log.e(TAG, "Cerebral Cortex API call is null: " + dsc.getDs_id());
                            break;
                        }
                        CerebralCortexDataResponse ccdr = LoganSquare.parse(dataResult, CerebralCortexDataResponse.class);
                        if (ccdr.status.contains("ok")) {
                            File newFile = new File(files[i].getAbsolutePath().replace(".csv.gz", "_archive.csv.gz"));
                            if (files[i].renameTo(newFile)) {
                                Log.d(TAG, "Successfully renamed file: " + files[i].getAbsolutePath());
                            }
                        } else if (ccdr.status.contains("error") && ccdr.message.contains("Unrecoverable")) {
                            File newFile = new File(files[i].getAbsolutePath().replace(".csv.gz", "_corrupt.csv.gz"));
                            if (files[i].renameTo(newFile)) {
                                Log.d(TAG, "Successfully renamed corrupt file: " + files[i].getAbsolutePath());
                            }
                        }
                        Log.d(TAG, "Uploaded Raw count: " + ccdr.count);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


    }

    private CerebralCortexDataSourceResponse registerDataSource(UserInfoCCResponse uiResponse, DataSourceClient dsc) {
        CerebralCortexDataSource ccdp = new CerebralCortexDataSource(uiResponse.id, dsc.getDataSource());
        String dataSourceResult = null;
        CerebralCortexDataSourceResponse result = new CerebralCortexDataSourceResponse();
        result.status = "ERROR";

        String data = new GsonBuilder().setPrettyPrinting().create().toJson(dsc);
        String filename = dsc.getDs_id() + "_datasource.json.gz";
        archiveJsonData(data, dsc.getDs_id(), filename);

        try {
            dataSourceResult = cerebralCortexAPI(requestURL + "datasources/register", gson.toJson(ccdp));
        } catch (IOException e) {
            Log.e("CerebralCortex", "Datasource registration error: " + e);

        }
        try {
            return LoganSquare.parse(dataSourceResult, CerebralCortexDataSourceResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
        }


        return result;
    }

    private StudyInfoCCResponse registerStudy(String encodedString) throws IOException {
        String siResult = null;
        siResult = cerebralCortexAPI(requestURL + "studies", encodedString);
        if (siResult == null) {
            return null;
        }
        return gson.fromJson(siResult, StudyInfoCCResponse.class);
    }

    private UserInfoCCResponse registerUser(String encodedString) throws IOException {
        String uiResult = null;
        uiResult = cerebralCortexAPI(requestURL + "participants", encodedString);
        if (uiResult == null) {
            return null;
        }
        return gson.fromJson(uiResult, UserInfoCCResponse.class);
    }

    @Nullable
    private StudyInfo getStudyInfo(DatabaseLogger dbLogger) throws IOException {
        DataSourceBuilder studyinfoBuilder = new DataSourceBuilder();
        studyinfoBuilder.setType(DataSourceType.STUDY_INFO);
        List<DataSourceClient> studyInfoClients = dbLogger.find(studyinfoBuilder.build());
        DataType si = null;
        for (DataSourceClient dsc : studyInfoClients) {
            List<DataType> studyInfo = dbLogger.query(dsc.getDs_id(), 1);
            for (DataType dt : studyInfo) {
                si = dt;
            }
        }
        if (si != null) {
            return LoganSquare.parse(((DataTypeJSONObject) si).getSample().toString(), StudyInfo.class);
        } else {
            return null;
        }
    }

    @Nullable
    private UserInfo getUserInfo(DatabaseLogger dbLogger) throws IOException {
        DataSourceBuilder userInfoBuilder = new DataSourceBuilder();
        userInfoBuilder.setType(DataSourceType.USER_INFO);
        List<DataSourceClient> userInfoClients = dbLogger.find(userInfoBuilder.build());
        DataType ui = null;
        for (DataSourceClient dsc : userInfoClients) {
            List<DataType> userInfo = dbLogger.query(dsc.getDs_id(), 1);
            for (DataType dt : userInfo) {
                ui = dt;
            }
        }
        if (ui != null) {
            return LoganSquare.parse(((DataTypeJSONObject) ui).getSample().toString(), UserInfo.class);
        } else {
            return null;
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

    public void run() {
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


        if (CCDIR != null) {
            File ccdir = new File(CCDIR);
            if (!ccdir.exists())
                ccdir.mkdirs();
        }
        messenger("Starting publish procedure");

        UserInfo uInfo = null;
        StudyInfo sInfo = null;
        try {
            uInfo = getUserInfo(dbLogger);
            sInfo = getStudyInfo(dbLogger);
        } catch (IOException e) {
            e.printStackTrace();
            messenger("uInfo or sInfo null");
            return;
        }

        if (uInfo != null) {
            if (uInfo.user_id.contentEquals("") || uInfo.uuid.contentEquals("")) {
                messenger("uInfo field is empty");
                return;
            }
        } else {
            messenger("User does not exist");
            return;
        }
        if (sInfo != null) {
            if (sInfo.id.contentEquals("") || sInfo.name.contentEquals("")) {
                messenger("sInfo field is empty");
                return;
            }
        } else {
            messenger("Study not defined");
            return;
        }
        messenger("Extracted user and study info");

        //Register User
        UserInfoCCResponse uiResponse = null;
        try {
            uiResponse = registerUser(gson.toJson(new UserInfoCC(uInfo)));
        } catch (IOException e) {
            messenger("User Info Registration Error");
            Log.e("CerebralCortex", "User Info Registration failed");
            e.printStackTrace();
            return;
        }
        messenger("Registered user");
        lastUpload = System.currentTimeMillis();

        //Register Study
        StudyInfoCCResponse siResponse = null;
        try {
            siResponse = registerStudy(gson.toJson(new StudyInfoCC(sInfo)));
        } catch (IOException e) {
            messenger("Study Info Registration Error");
            Log.e("CerebralCortex", "Study Info Registration failed");
            e.printStackTrace();
            return;
        }

        if (siResponse == null || uiResponse == null) {
            messenger("Registration has failed");
            Log.e("CerebralCortex", "Registration failed");
            return;
        }
        messenger("Registered study");

        //Register Participant in Study
        ParticipantRegistration pr = new ParticipantRegistration(siResponse.id, uiResponse.id);
        String prResult = null;
        try {
            prResult = cerebralCortexAPI(requestURL + "studies/register_participant", gson.toJson(pr));
        } catch (IOException e) {
            Log.e("CerebralCortex", "Register participant error: " + e);
            Log.e("CerebralCortex", prResult);
            return;
        }
        if (prResult == null) {
            messenger("prResult is null");
            return;
        }
        if (prResult.contains("Invalid participant or study id")) {
            messenger("Register participant error: ");
            Log.e("CerebralCortex", prResult);
            return;
        }
        messenger("Registered participant in study");

        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        List<DataSourceClient> dataSourceClients = dbLogger.find(dataSourceBuilder.build());


        Map<DataSourceClient, CerebralCortexDataSourceResponse> validDataSources = new HashMap<>();
        for (DataSourceClient dsc : dataSourceClients) {
            CerebralCortexDataSourceResponse ccdpResponse = registerDataSource(uiResponse, dsc);
            if (ccdpResponse.status.contains("ok") && !inRestrictedList(dsc)) {
                messenger("Registered datastream: " + dsc.getDs_id() + " (" + dsc.getDataSource().getId() + ":" + dsc.getDataSource().getType() + ")");
                validDataSources.put(dsc, ccdpResponse);
            }
        }

        for (Map.Entry<DataSourceClient, CerebralCortexDataSourceResponse> entry : validDataSources.entrySet()) {

            messenger("Publishing data for " + entry.getKey().getDs_id() + " (" + entry.getKey().getDataSource().getId() + ":" + entry.getKey().getDataSource().getType() + ") to " + entry.getValue().datastream_id);
            publishDataStream(entry.getKey(), entry.getValue(), dbLogger);
        }

        for (Map.Entry<DataSourceClient, CerebralCortexDataSourceResponse> entry : validDataSources.entrySet()) {

            messenger("Publishing raw data for " + entry.getKey().getDs_id() + " (" + entry.getKey().getDataSource().getId() + ":" + entry.getKey().getDataSource().getType() + ") to " + entry.getValue().datastream_id);
            publishDataFiles(entry.getKey(), entry.getValue());
        }

        messenger("Upload Complete");
    }


    /**
     * Upload method for publishing data to the Cerebral Cortex webservice
     *
     * @param requestURL URL
     * @param json       String of data to send to Cerebral Cortex
     */
    public String cerebralCortexAPI(String requestURL, String json) throws IOException {
        long totalst = System.currentTimeMillis();
        String result = null;

        RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);


        Request request = new Request.Builder()
                .url(requestURL)
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = gzipClient.newCall(request).execute();

        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        result = response.body().string();

        Log.d("TIMING", "CerebralCortexAPI CALL: " + (System.currentTimeMillis() - totalst));

        return result;

    }

    /**
     * Upload method for publishing data to the Cerebral Cortex webservice
     *
     * @param requestURL URL
     */
    public String cerebralCortexAPI_RAWFile(String requestURL, Integer datastreamID, File file) throws IOException {
        long totalst = System.currentTimeMillis();
        String result = null;

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("datastream_id", datastreamID.toString())
                .addFormDataPart("rawdatafile", file.getName(),
                        RequestBody.create(MediaType.parse(""), file))
                .build();

        Request request = new Request.Builder()
                .url(requestURL)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .build();

        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

        result = response.body().string();

        Log.d("TIMING", "CerebralCortexAPI_RawFile CALL: " + (System.currentTimeMillis() - totalst));

        return result;
    }
}
