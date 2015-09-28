package org.md2k.datakit.logger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.utilities.Report.Log;

import java.util.ArrayList;

/**
 * Copyright (c) 2015, The University of Memphis, MD2K Center
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
public class DatabaseTable_Data {
    private static final String TAG = DatabaseTable_Data.class.getSimpleName();
    private static String TABLE_NAME = "data";
    private static String C_ID = "_id";
    private static String C_DATASOURCE_ID = "datasource_id";
    private static String C_DATETIME = "datetime";
    private static String C_SAMPLE="sample";
    ArrayList<ContentValues> cValues = new ArrayList<ContentValues>();
    long lastUnlock=0;
    private static long WAITTIME = 2 * 1000L; // 2 second;

    private static final String SQL_CREATE_DATA = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + C_ID + " INTEGER PRIMARY KEY autoincrement, " +
            C_DATASOURCE_ID + " TEXT, " + C_DATETIME + " LONG, " +
            C_SAMPLE + " BLOB not null);";


    DatabaseTable_Data(SQLiteDatabase db) {
        Log.d(TAG, "DatabaseTable_Data");

        createIfNotExists(db);
    }

    public void createIfNotExists(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DATA);
    }
    private void insertDB(SQLiteDatabase db){

        Log.d(TAG,"DataBaseLogger() db isopen="+db.isOpen()+" readonly="+db.isReadOnly()+" isWriteAheadLoggingEnabled="+db.isWriteAheadLoggingEnabled());
        if(cValues.size()==0) return;
//        if(!db.isOpen()) return;
        db.beginTransaction();
        for (int i = 0; i < cValues.size(); i++)
            db.insert(TABLE_NAME, null, cValues.get(i));
        cValues.clear();
        try {
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }
    }

    public void insert(SQLiteDatabase db, int dataSourceId, DataType dataType) {
        ContentValues contentValues=prepareData(dataSourceId, dataType);
        Log.d(TAG,"insert ...");
        Log.d(TAG,"DataBaseLogger() db isopen="+db.isOpen()+" readonly="+db.isReadOnly()+" isWriteAheadLoggingEnabled="+db.isWriteAheadLoggingEnabled());
        cValues.add(contentValues);
        if (dataType.getDateTime() - lastUnlock >= WAITTIME) {
            Log.d(TAG,"update db()");
            insertDB(db);
            lastUnlock=dataType.getDateTime();
        }
        Log.d(TAG,"...insert");
    }
    private String[] prepareSelectionArgs(int ds_id,long starttimestamp,long endtimestamp) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(ds_id));
        selectionArgs.add(String.valueOf(starttimestamp));
        selectionArgs.add(String.valueOf(endtimestamp));
        String[] stringArray = selectionArgs.toArray(new String[selectionArgs.size()]);
        return stringArray;
    }
    private String[] prepareSelectionArgs(int ds_id) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(ds_id));
        String[] stringArray = selectionArgs.toArray(new String[selectionArgs.size()]);
        return stringArray;
    }

    private String prepareSelection() {
        String selection = "";
        selection=C_DATASOURCE_ID+"=? AND "+C_DATETIME+" >=? AND "+C_DATETIME+" <=?";
        return selection;
    }
    private String prepareSelectionLastSamples() {
        String selection = "";
        selection=C_DATASOURCE_ID+"=?";
        return selection;
    }

    public ArrayList<DataType> query(SQLiteDatabase db, int ds_id, long starttimestamp,long endtimestamp){
        ArrayList<DataType> dataTypes = new ArrayList<>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);
        String[] columns = new String[]{C_SAMPLE};
        String selection = prepareSelection();
        String[] selectionArgs = prepareSelectionArgs(ds_id,starttimestamp,endtimestamp);
        Cursor mCursor = db.query(TABLE_NAME,
                columns, selection, selectionArgs, null, null, null);
        if (mCursor.moveToFirst()) {
            do {
                dataTypes.add(DataType.fromBytes(mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE))));
            } while (mCursor.moveToNext());
        }
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        return dataTypes;
    }
    public ArrayList<DataType> query(SQLiteDatabase db, int ds_id, int last_n_sample){
        Log.d(TAG,"ds_id="+ds_id+" last_n_sample="+last_n_sample);
        ArrayList<DataType> dataTypes = new ArrayList<>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(TABLE_NAME);
        String[] columns = new String[]{C_SAMPLE};
        String selection = prepareSelectionLastSamples();
        String[] selectionArgs = prepareSelectionArgs(ds_id);
        Cursor mCursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null,"_id DESC", String.valueOf(last_n_sample));
        if (mCursor.moveToFirst()) {
            do {
                dataTypes.add(DataType.fromBytes(mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE))));
            } while (mCursor.moveToNext());
        }
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
        Log.d(TAG,"datatype length="+dataTypes.size());
        return dataTypes;
    }

    public ContentValues prepareData(int dataSourceId, DataType dataType) {
        ContentValues contentValues=new ContentValues();
        byte[] dataTypeArray = dataType.toBytes();
        long startDateTime = dataType.getDateTime();

        contentValues.put(C_DATASOURCE_ID, dataSourceId);
        contentValues.put(C_DATETIME, startDateTime);
        contentValues.put(C_SAMPLE, dataTypeArray);
        return contentValues;
    }
    public void commit(SQLiteDatabase db){
        insertDB(db);
    }
}
