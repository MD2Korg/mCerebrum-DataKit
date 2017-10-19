package org.md2k.datakit.logger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.status.Status;
import org.md2k.datakitapi.time.DateTime;
import org.md2k.utilities.Report.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

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
public class DatabaseTable_DataSource {
    private static String TABLE_NAME = "datasources";
    private static String C_DS_ID = "ds_id";
    private static String C_DATASOURCE_ID = "datasource_id";
    private static String C_DATASOURCE_TYPE = "datasource_type";
    private static String C_PLATFORM_ID = "platform_id";
    private static String C_PLATFROM_TYPE = "platform_type";
    private static String C_PLATFORMAPP_ID = "platformapp_id";
    private static String C_PLATFROMAPP_TYPE = "platformapp_type";
    private static String C_APPLICATION_ID = "application_id";
    private static String C_APPLICATION_TYPE = "application_type";
    private static String C_CREATEDATETIME = "create_datetime";
    private static String C_DATASOURCE = "datasource";
    private static final String SQL_CREATE_DATASOURCE = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + C_DS_ID + " INTEGER PRIMARY KEY autoincrement, " +
            C_DATASOURCE_ID + " TEXT, " + C_DATASOURCE_TYPE + " TEXT, " +
            C_PLATFORM_ID + " TEXT, " + C_PLATFROM_TYPE + " TEXT," +
            C_PLATFORMAPP_ID + " TEXT, " + C_PLATFROMAPP_TYPE + " TEXT," +
            C_APPLICATION_ID + " TEXT, " + C_APPLICATION_TYPE + " TEXT," +
            C_CREATEDATETIME + " LONG, " + C_DATASOURCE + " BLOB not null);";

    DatabaseTable_DataSource(SQLiteDatabase db) {
        createIfNotExists(db);
    }

    private synchronized String[] prepareSelectionArgs(DataSource dataSource) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        if (dataSource.getId() != null) selectionArgs.add(dataSource.getId());
        if (dataSource.getType() != null) selectionArgs.add(dataSource.getType());
        if (dataSource.getPlatform() != null && dataSource.getPlatform().getId() != null)
            selectionArgs.add(dataSource.getPlatform().getId());
        if (dataSource.getPlatform() != null && dataSource.getPlatform().getType() != null)
            selectionArgs.add(dataSource.getPlatform().getType());
        if (dataSource.getPlatformApp() != null && dataSource.getPlatformApp().getId() != null)
            selectionArgs.add(dataSource.getPlatformApp().getId());
        if (dataSource.getPlatformApp() != null && dataSource.getPlatformApp().getType() != null)
            selectionArgs.add(dataSource.getPlatformApp().getType());
        if (dataSource.getApplication() != null && dataSource.getApplication().getId() != null)
            selectionArgs.add(dataSource.getApplication().getId());
        if (dataSource.getApplication() != null && dataSource.getApplication().getType() != null)
            selectionArgs.add(dataSource.getApplication().getType());
        if (selectionArgs.size() == 0) return null;
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    private synchronized String prepareSelection(DataSource dataSource) {
        String selection = "";
        if (dataSource.getId() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_DATASOURCE_ID + "=?";
        }
        if (dataSource.getType() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_DATASOURCE_TYPE + "=?";
        }
        if (dataSource.getPlatform() != null && dataSource.getPlatform().getId() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_PLATFORM_ID + "=?";
        }
        if (dataSource.getPlatform() != null && dataSource.getPlatform().getType() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_PLATFROM_TYPE + "=?";
        }
        if (dataSource.getPlatformApp() != null && dataSource.getPlatformApp().getId() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_PLATFORMAPP_ID + "=?";
        }
        if (dataSource.getPlatformApp() != null && dataSource.getPlatformApp().getType() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_PLATFROMAPP_TYPE + "=?";
        }
        if (dataSource.getApplication() != null && dataSource.getApplication().getId() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_APPLICATION_ID + "=?";
        }
        if (dataSource.getApplication() != null && dataSource.getApplication().getType() != null) {
            if (!selection.equals("")) selection += " AND ";
            selection += C_APPLICATION_TYPE + "=?";
        }
        if (selection.equals("")) return null;
        return selection;
    }

    public synchronized void createIfNotExists(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DATASOURCE);
    }

    public synchronized ArrayList<DataSourceClient> findDataSource(SQLiteDatabase db, DataSource dataSource) {
        ArrayList<DataSourceClient> dataSourceClients = new ArrayList<>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);
        String[] columns = new String[]{C_DS_ID, C_DATASOURCE};
        String selection = prepareSelection(dataSource);
        String[] selectionArgs = prepareSelectionArgs(dataSource);
        Cursor mCursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);
        try {
            if (mCursor.moveToFirst()) {
                do {
                    byte[] bytes = mCursor.getBlob(mCursor.getColumnIndex(C_DATASOURCE));
                    DataSource curDataSource = fromBytes(bytes);
                    DataSourceClient dataSourceClient = new DataSourceClient(mCursor.getInt(mCursor.getColumnIndex(C_DS_ID)),
                            curDataSource, new Status(Status.DATASOURCE_EXIST));
//                    DataSourceClient dataSourceClient = new DataSourceClient(mCursor.getInt(mCursor.getColumnIndex(C_DS_ID)),
//                            DataSource.fromBytes(mCursor.getBlob(mCursor.getColumnIndex(C_DATASOURCE))), new Status(Status.DATASOURCE_EXIST));
                    dataSourceClients.add(dataSourceClient);
                } while (mCursor.moveToNext());
            }
            mCursor.close();
        }catch (Exception ignored){

        }
        return dataSourceClients;
    }

    public synchronized void removeAll(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
    }

    public synchronized DataSourceClient register(SQLiteDatabase db, DataSource dataSource) {
        ContentValues cValues = prepareDataSource(dataSource);
        int newRowId;
        newRowId = (int) db.insert(TABLE_NAME, null, cValues);
        if (newRowId == -1) {
            return new DataSourceClient(-1, dataSource, new Status(Status.INTERNAL_ERROR));
        } else return new DataSourceClient(newRowId, dataSource, new Status(Status.SUCCESS));
    }

    public synchronized ContentValues prepareDataSource(DataSource dataSource) {
//        byte[] dataSourceArray = dataSource.toBytes();
        byte[] dataSourceArray = toBytes(dataSource);

        long curTime = DateTime.getDateTime();
        ContentValues cValues = new ContentValues();
        if (dataSource.getId() != null) cValues.put(C_DATASOURCE_ID, dataSource.getId());
        if (dataSource.getType() != null) cValues.put(C_DATASOURCE_TYPE, dataSource.getType());
        if (dataSource.getPlatform() != null && dataSource.getPlatform().getId() != null)
            cValues.put(C_PLATFORM_ID, dataSource.getPlatform().getId());
        if (dataSource.getPlatform() != null && dataSource.getPlatform().getType() != null)
            cValues.put(C_PLATFROM_TYPE, dataSource.getPlatform().getType());
        if (dataSource.getPlatformApp() != null && dataSource.getPlatformApp().getId() != null)
            cValues.put(C_PLATFORMAPP_ID, dataSource.getPlatformApp().getId());
        if (dataSource.getPlatformApp() != null && dataSource.getPlatformApp().getType() != null)
            cValues.put(C_PLATFROMAPP_TYPE, dataSource.getPlatformApp().getType());
        if (dataSource.getApplication() != null && dataSource.getApplication().getId() != null)
            cValues.put(C_APPLICATION_ID, dataSource.getApplication().getId());
        if (dataSource.getApplication() != null && dataSource.getApplication().getType() != null)
            cValues.put(C_APPLICATION_TYPE, dataSource.getApplication().getType());
        cValues.put(C_CREATEDATETIME, curTime);
        cValues.put(C_DATASOURCE, dataSourceArray);
        return cValues;
    }

    private synchronized byte[] toBytes(DataSource dataSource) {
        Kryo kryo=new Kryo();
        byte[] bytes;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output, dataSource);
        output.close();
        bytes = baos.toByteArray();
        return bytes;
    }

    private synchronized DataSource fromBytes(byte[] bytes) {
        Kryo kryo=new Kryo();
        Input input = new Input(new ByteArrayInputStream(bytes));
        DataSource curDataSource = (DataSource) kryo.readClassAndObject(input);
        input.close();
        return curDataSource;
    }

}
