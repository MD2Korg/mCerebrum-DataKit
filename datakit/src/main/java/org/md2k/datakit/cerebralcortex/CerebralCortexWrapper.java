package org.md2k.datakit.cerebralcortex;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.bluelinelabs.logansquare.LoganSquare;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.entity.StringEntityHC4;
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
import org.md2k.datakit.logger.DatabaseLogger;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeJSONObject;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceBuilder;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.source.datasource.DataSourceType;
import org.md2k.utilities.Report.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;


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

public class CerebralCortexWrapper extends AsyncTask<Void, Integer, Boolean> {
    private static final String TAG = CerebralCortexWrapper.class.getSimpleName();
    private static final Integer COUNT_INDEX = -1;
    HashMap<Integer, Long> keySyncState = new HashMap<Integer, Long>();
    DatabaseLogger dbLogger = null;
    private Context context;
    private String requestURL;
    private List<DataSource> restricted;
    private Gson gson = new GsonBuilder().serializeNulls().create();

    public CerebralCortexWrapper(Context context, String url, List<DataSource> restricted) throws IOException {
        this.context = context;
        this.requestURL = url;
        this.restricted = restricted;
        dbLogger = DatabaseLogger.getInstance(context);
    }

    private static String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d("readStream", builder.toString());

        return builder.toString();
    }

    private void messenger(String message) {
        Intent intent = new Intent(Constants.CEREBRAL_CORTEX_STATUS);
        intent.putExtra("CC_Upload", message);
        Log.d("CerebralCortexMessenger", message);
        LocalBroadcastManager.getInstance(this.context).sendBroadcast(intent);
    }

    private void saveHashMap(HashMap<Integer, Long> keys) {

        //Record size of database for future reference
        if (dbLogger == null) {
            try {
                dbLogger = DatabaseLogger.getInstance(this.context);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        DataTypeLong count = dbLogger.querySize();
        keys.put(COUNT_INDEX, count.getSample());

        Gson gson = new GsonBuilder().create();
        try {
            FileOutputStream output = context.openFileOutput(Constants.KEYHASHMAP, Context.MODE_PRIVATE);
            output.write(gson.toJson(keys).getBytes());
            output.close();
        } catch (IOException e) {
            //Not concerned, just a missing file
        }
    }

    private HashMap<Integer, Long> readHashMap() {
        Gson gson = new GsonBuilder().create();

        String inputData = "";
        String line;
        try {
            FileInputStream in = context.openFileInput(Constants.KEYHASHMAP);
            InputStreamReader inputStreamReader = new InputStreamReader(in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            while ((line = bufferedReader.readLine()) != null)
                inputData += line;
            bufferedReader.close();
            inputStreamReader.close();
            in.close();
        } catch (IOException e) {
            //Not concerned, missing file will be created below
        }

        Type hashType = new TypeToken<Map<Integer, Long>>() {
        }.getType();
        HashMap<Integer, Long> result = gson.fromJson(inputData, hashType);

        if (result == null)
            return new HashMap<>();

        if (dbLogger == null) {
            try {
                dbLogger = DatabaseLogger.getInstance(this.context);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        DataTypeLong count = dbLogger.querySize();
        if (result.containsKey(COUNT_INDEX) && result.get(COUNT_INDEX) > count.getSample()) {
            //Reset DB tracking pointers
            return new HashMap<>();
        }

        return result;
    }

    private boolean publishDataKitData() {

        //Ensure that the database is available
        if (dbLogger == null) {
            try {
                dbLogger = DatabaseLogger.getInstance(this.context);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        messenger("Starting publish procedure");

        keySyncState = readHashMap();

        UserInfo uInfo = null;
        StudyInfo sInfo = null;
        try {
            uInfo = getUserInfo();
            sInfo = getStudyinfo();
        } catch (IOException e) {
            e.printStackTrace();
            messenger("uInfo or sInfo null");
            return false;
        }

        if (uInfo != null) {
            if (uInfo.user_id.contentEquals("") || uInfo.uuid.contentEquals("")) {
                messenger("uInfo field contains a blank");
                return false;
            }
        } else {
            return false;
        }
        if (sInfo != null) {
            if (sInfo.id.contentEquals("") || sInfo.name.contentEquals("")) {
                messenger("sInfo field contains a blank");
                return false;
            }
        } else {
            return false;
        }
        messenger("Extracted user and study info");

        //Register User
        UserInfoCCResponse uiResponse = null;
        try {
            uiResponse = registerUser(gson.toJson(new UserInfoCC(uInfo)));
        } catch (IOException e) {
            messenger("User Info Registration Error");
            e.printStackTrace();
            return false;
        }
        messenger("Registered user");

        //Register Study
        StudyInfoCCResponse siResponse = null;
        try {
            siResponse = registerStudy(gson.toJson(new StudyInfoCC(sInfo)));
        } catch (IOException e) {
            messenger("User Info Registration Error");
            e.printStackTrace();
            return false;
        }

        if (siResponse == null || uiResponse == null) {
            messenger("Registration has failed");
            return false;
        }
        messenger("Registered study");

        //Register Participant in Study
        ParticipantRegistration pr = new ParticipantRegistration(siResponse.id, uiResponse.id);
        String prResult = null;
        try {
            prResult = cerebralCortexAPI(requestURL + "studies/register_participant", gson.toJson(pr));
        } catch (IOException e) {
            Log.d("CerebralCortex", "Register participant error: " + e);
            Log.d("CerebralCortex", prResult);
            return false;
        }
        if (prResult == null) {
            messenger("prResult is null");
            return false;
        }
        if (prResult.contains("Invalid participant or study id")) {
            messenger("Register participant error: ");
            Log.d("CerebralCortex", prResult);
            return false;
        }
        messenger("Registered participant in study");

        DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
        List<DataSourceClient> dataSourceClients = dbLogger.find(dataSourceBuilder.build());


        Map<DataSourceClient, CerebralCortexDataSourceResponse> validDataSources = new HashMap<>();
        for (DataSourceClient dsc : dataSourceClients) {
            CerebralCortexDataSourceResponse ccdpResponse = registerDataSource(uiResponse, dsc);
            if (ccdpResponse.status.contains("ok") && !restricted.contains(dsc)) {
                messenger("Registered datastream: " + dsc.getDs_id());
                validDataSources.put(dsc, ccdpResponse);
                if (!keySyncState.containsKey(dsc.getDs_id())) {
                    keySyncState.put(dsc.getDs_id(), 0L);
                }
            }
        }
        for (Map.Entry<DataSourceClient, CerebralCortexDataSourceResponse> entry : validDataSources.entrySet()) {

            messenger("Publishing data for " + entry.getKey().getDs_id());
            publishDataStream(false, entry.getKey(), entry.getValue(), keySyncState);
            publishDataStream(true, entry.getKey(), entry.getValue(), keySyncState);

        }

        messenger("Upload Complete");
        return true;
        }

    private void publishDataStream(boolean hf, DataSourceClient dsc, CerebralCortexDataSourceResponse ccdpResponse, HashMap<Integer, Long> keySyncState) {
        String APIendpoint;
        String dataResult = null;
        boolean cont = true;

        while (cont) {
            cont = false;

            CerebralCortexData ccdata = new CerebralCortexData(ccdpResponse.datastream_id);

            //Computed Data Store
            List<RowObject> objects;
            int BLOCK_SIZE_LIMIT;
            if (!hf) {
                APIendpoint = "datapoints/bulkload";
                objects = dbLogger.queryLastKey(dsc.getDs_id(), keySyncState.get(dsc.getDs_id()), Constants.DATA_BLOCK_SIZE_LIMIT);
                BLOCK_SIZE_LIMIT = Constants.DATA_BLOCK_SIZE_LIMIT;
            } else {
                APIendpoint = "rawdatapoints/bulkload";
                objects = dbLogger.queryHFLastKey(dsc.getDs_id(), keySyncState.get(dsc.getDs_id()), Constants.HF_DATA_BLOCK_SIZE_LIMIT);
                BLOCK_SIZE_LIMIT = Constants.HF_DATA_BLOCK_SIZE_LIMIT;
            }

            if (objects.size() > 0) {
                Log.d("CerebralCortex", "Offloading data for " + dsc.getDs_id());
                long lastKeyIndex = keySyncState.get(dsc.getDs_id());

                for (RowObject obj : objects) {
                    ccdata.data.add(obj.toArrayForm());
                    lastKeyIndex = obj.rowKey;
                }

                try {
                    String data = gson.toJson(ccdata);
                    dataResult = cerebralCortexAPI(requestURL + APIendpoint, data);
                    if (dataResult == null) {
                        break;
                    }
                    CerebralCortexDataResponse ccdr = LoganSquare.parse(dataResult, CerebralCortexDataResponse.class);
                    if (ccdr.count > 0) {
                        keySyncState.put(dsc.getDs_id(), lastKeyIndex);
                        Log.d("CerebralCortex", "Record Upload Count: (" + dsc.getDs_id() + ") #" + ccdr.count);
                        onProgressUpdate(dsc.getDs_id(), (int) lastKeyIndex);
                    }
                    if (ccdr.count == BLOCK_SIZE_LIMIT) {
                        cont = true;
                    }
                } catch (IOException e) {
                    Log.d("CerebralCortex", "Bulk load error: " + e);
                    messenger("Bulk load error");
                    break;
                }
            }
            saveHashMap(keySyncState);
        }
    }


    private CerebralCortexDataSourceResponse registerDataSource(UserInfoCCResponse uiResponse, DataSourceClient dsc) {
        CerebralCortexDataSource ccdp = new CerebralCortexDataSource(uiResponse.id, dsc.getDataSource());
        String dataSourceResult = null;
        CerebralCortexDataSourceResponse result = new CerebralCortexDataSourceResponse();
        result.status = "ERROR";
        try {
            dataSourceResult = cerebralCortexAPI(requestURL + "datasources/register", gson.toJson(ccdp));
        } catch (IOException e) {
            Log.d("CerebralCortex", "Datasource registration error: " + e);

        }
        try {
            return LoganSquare.parse(dataSourceResult, CerebralCortexDataSourceResponse.class);
        } catch (IOException e) {
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
    private StudyInfo getStudyinfo() throws IOException {
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
    private UserInfo getUserInfo() throws IOException {
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

    @Override
    protected void onProgressUpdate(Integer... ints) {
        Log.d("CerebralCortex", "Progress Update:" + ints[0]);
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        return publishDataKitData();
    }

    /**
     * Upload method for publishing data to the Cerebral Cortex webservice
     *
     * @param requestURL URL
     * @param json    String of data to send to Cerebral Cortex
     */
    public String cerebralCortexAPI(String requestURL, String json) throws IOException {
        long totalst = System.currentTimeMillis();
        String result = null;


        GzipCompressingEntity entity = new GzipCompressingEntity(new StringEntityHC4(json));

        URL url = new URL(requestURL);

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
//            urlConnection.setChunkedStreamingMode(0);

            urlConnection.setConnectTimeout(60000);
            urlConnection.setReadTimeout(60000);

            urlConnection.setRequestProperty("Content-Encoding", "gzip");
            urlConnection.setRequestProperty("Accept", "application/json");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setRequestProperty("Connection", "Keep-Alive");
            urlConnection.setRequestProperty("Cache-Control", "no-cache");

            urlConnection.setRequestProperty("Content-Length", "" + entity.getContentLength());

            entity.writeTo(urlConnection.getOutputStream());


            if (urlConnection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                return readStream(urlConnection.getInputStream());
            }
        } catch (Exception e) {
            Log.e("Cerebral Cortx API", "POST Error: " + e + "(" + requestURL + ")");
        } finally {
            urlConnection.disconnect();
        }
        Log.d("TIMING", "CerebralCortexAPI CALL: " + (System.currentTimeMillis() - totalst));

        return result;
    }


}
