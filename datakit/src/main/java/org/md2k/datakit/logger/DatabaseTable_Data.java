package org.md2k.datakit.logger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.status.Status;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/*
 * Copyright (c) 2015, The University of Memphis, MD2K Center
 * - Syed Monowar Hossain <monowar.hossain@gmail.com>
 * - Timothy W. Hnat <twhnat@memphis.edu>
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
    private static String HIGHFREQ_TABLE_NAME = "rawdata";
    private static String C_ID = "_id";
    private static String C_DATASOURCE_ID = "datasource_id";
    private static final String SQL_CREATE_DATA_INDEX = "CREATE INDEX IF NOT EXISTS index_datasource_id on " + TABLE_NAME + " (" + C_DATASOURCE_ID + ");";
    private static final String SQL_CREATE_HIGHFREQ_DATA_INDEX = "CREATE INDEX IF NOT EXISTS index_hf_datasource_id on " + HIGHFREQ_TABLE_NAME + " (" + C_DATASOURCE_ID + ");";
    private static String C_DATETIME = "datetime";
    private static String C_SAMPLE = "sample";
    private static final String SQL_CREATE_DATA = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + C_ID + " INTEGER PRIMARY KEY autoincrement, " +
            C_DATASOURCE_ID + " TEXT, " + C_DATETIME + " LONG, " +
            C_SAMPLE + " BLOB not null);";
    private static final String SQL_CREATE_HIGHFREQ_DATA = "CREATE TABLE IF NOT EXISTS " + HIGHFREQ_TABLE_NAME + " (" + C_ID + " INTEGER PRIMARY KEY autoincrement, " +
            C_DATASOURCE_ID + " TEXT, " + C_DATETIME + " LONG, " +
            C_SAMPLE + " BLOB not null);";
    private static String C_COUNT = "c";
    private static long WAITTIME = 5 * 1000L; // 5 second;
    ArrayList<ContentValues> cValues = new ArrayList<ContentValues>();
    ArrayList<ContentValues> hfValues = new ArrayList<ContentValues>();
    long lastUnlock = 0;
    Kryo kryo;


    DatabaseTable_Data(SQLiteDatabase db) {
        kryo = new Kryo();
        createIfNotExists(db);
    }

    public void removeAll(SQLiteDatabase db) {
        db.execSQL("DROP INDEX index_datasource_id");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("DROP INDEX index_hf_datasource_id");
        db.execSQL("DROP TABLE IF EXISTS " + HIGHFREQ_TABLE_NAME);
    }

    public void createIfNotExists(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_DATA);
        db.execSQL(SQL_CREATE_DATA_INDEX);
        db.execSQL(SQL_CREATE_HIGHFREQ_DATA);
        db.execSQL(SQL_CREATE_HIGHFREQ_DATA_INDEX);
    }

    private Status insertDB(SQLiteDatabase db, String tableName, List<ContentValues> data) {
        try {

            if (data.size() == 0)
                return new Status(Status.SUCCESS);

            db.beginTransaction();
            for (int i = 0; i < data.size(); i++)
                db.insert(tableName, null, data.get(i));
            data.clear();
            try {
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception e) {
            return new Status(Status.INTERNAL_ERROR);
        }
        return new Status(Status.SUCCESS);
    }


    public Status insert(SQLiteDatabase db, int dataSourceId, DataType dataType) {
        Status status = new Status(Status.SUCCESS);
        ContentValues contentValues = prepareData(dataSourceId, dataType);
        cValues.add(contentValues);
        if (dataType.getDateTime() - lastUnlock >= WAITTIME) {
            status = insertDB(db, TABLE_NAME, cValues);
            lastUnlock = dataType.getDateTime();
        }
        return status;
    }

    public Status insertHF(SQLiteDatabase db, int dataSourceId, DataTypeDoubleArray dataType) {
        Status status = new Status(Status.SUCCESS);
        ContentValues contentValues = prepareDataHF(dataSourceId, dataType);
        hfValues.add(contentValues);
        if (dataType.getDateTime() - lastUnlock >= WAITTIME) {
            status = insertDB(db, HIGHFREQ_TABLE_NAME, hfValues);
            lastUnlock = dataType.getDateTime();
        }
        return status;
    }

    private String[] prepareSelectionArgs(int ds_id, long starttimestamp, long endtimestamp) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(ds_id));
        selectionArgs.add(String.valueOf(starttimestamp));
        selectionArgs.add(String.valueOf(endtimestamp));
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    private String[] prepareSelectionArgs(int ds_id) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(ds_id));
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    private String[] prepareLastKeySelectionArgs(int ds_id, long last_key) {
        ArrayList<String> selectionArgs = new ArrayList<>();
        selectionArgs.add(String.valueOf(ds_id));
        selectionArgs.add(String.valueOf(last_key));
        return selectionArgs.toArray(new String[selectionArgs.size()]);
    }

    private String prepareSelection() {
        String selection = "";
        selection = C_DATASOURCE_ID + "=? AND " + C_DATETIME + " >=? AND " + C_DATETIME + " <=?";
        return selection;
    }

    private String prepareSelectionLastSamples() {
        String selection = "";
        selection = C_DATASOURCE_ID + "=?";
        return selection;
    }

    private String prepareSelectionLastKey() {
        String selection = "";
        selection += C_ID + ">? AND ";
        selection += C_DATASOURCE_ID + "=?";
        return selection;
    }

    public ArrayList<DataType> query(SQLiteDatabase db, int ds_id, long starttimestamp, long endtimestamp) {
        insertDB(db, TABLE_NAME, cValues);
        ArrayList<DataType> dataTypes = new ArrayList<>();
        String[] columns = new String[]{C_SAMPLE};
        String selection = prepareSelection();
        String[] selectionArgs = prepareSelectionArgs(ds_id, starttimestamp, endtimestamp);
        Cursor mCursor = db.query(TABLE_NAME,
                columns, selection, selectionArgs, null, null, null);
        if (mCursor.moveToFirst()) {
            do {
                dataTypes.add(fromBytes(mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE))));
            } while (mCursor.moveToNext());
        }
        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        return dataTypes;
    }

    public ArrayList<DataType> query(SQLiteDatabase db, int ds_id, int last_n_sample) {
        insertDB(db, TABLE_NAME, cValues);
        ArrayList<DataType> dataTypes = new ArrayList<>();
        String[] columns = new String[]{C_SAMPLE};
        String selection = prepareSelectionLastSamples();
        String[] selectionArgs = prepareSelectionArgs(ds_id);
        Cursor mCursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, "_id DESC", String.valueOf(last_n_sample));
        if (mCursor.moveToFirst()) {
            do {
                dataTypes.add(fromBytes(mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE))));
            } while (mCursor.moveToNext());
        }
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
        return dataTypes;
    }

    public ArrayList<DataType> queryHFlastN(SQLiteDatabase db, int ds_id, int last_n_sample) {
        insertDB(db, HIGHFREQ_TABLE_NAME, cValues);
        ArrayList<DataType> dataTypes = new ArrayList<>();
        String sql = "select datetime, sample from rawdata where datasource_id=" + Integer.toString(ds_id) + " ORDER by _id DESC" + Integer.toString(last_n_sample);
        Cursor mCursor = db.rawQuery(sql, null);
        if (mCursor.moveToFirst()) {
            do {
                byte[] data = mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE));
                DataTypeDoubleArray dt = DataTypeDoubleArray.fromRawBytes(mCursor.getLong(mCursor.getColumnIndex(C_DATETIME)), data);
                dataTypes.add(dt);
            } while (mCursor.moveToNext());
        }
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
        return dataTypes;
    }


    public ArrayList<RowObject> queryLastKey(SQLiteDatabase db, int ds_id, long last_key, int limit) {
        insertDB(db, TABLE_NAME, cValues);
        ArrayList<RowObject> rowObjects = new ArrayList<>();
        String sql = "select _id, sample from data where _id>" + Long.toString(last_key) + " and datasource_id=" + Integer.toString(ds_id) + " LIMIT " + Integer.toString(limit);
        Cursor mCursor = db.rawQuery(sql, null);
        if (mCursor.moveToFirst()) {
            do {
                DataType dt = fromBytes(mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE)));
                rowObjects.add(new RowObject(mCursor.getLong(mCursor.getColumnIndex(C_ID)), dt));
            } while (mCursor.moveToNext());
        }
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
        return rowObjects;
    }

    public ArrayList<RowObject> queryHFLastKey(SQLiteDatabase db, int ds_id, long last_key, int limit) {
        insertDB(db, HIGHFREQ_TABLE_NAME, cValues);
        ArrayList<RowObject> rowObjects = new ArrayList<>();
        String sql = "select _id, datetime, sample from rawdata where _id>" + Long.toString(last_key) + " and datasource_id=" + Integer.toString(ds_id) + " LIMIT " + Integer.toString(limit);
        Cursor mCursor = db.rawQuery(sql, null);
        if (mCursor.moveToFirst()) {
            do {
                byte[] data = mCursor.getBlob(mCursor.getColumnIndex(C_SAMPLE));
                DataTypeDoubleArray dt = DataTypeDoubleArray.fromRawBytes(mCursor.getLong(mCursor.getColumnIndex(C_DATETIME)), data);
                rowObjects.add(new RowObject(mCursor.getLong(mCursor.getColumnIndex(C_ID)), dt));
            } while (mCursor.moveToNext());
        }
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
        return rowObjects;
    }

    public DataTypeLong querySize(SQLiteDatabase db) {
        insertDB(db, TABLE_NAME, cValues);
        String sql = "select count(_id)as c from data";
        Cursor mCursor = db.rawQuery(sql, null);
        DataTypeLong count = new DataTypeLong(0L, 0L);
        if (mCursor.moveToFirst()) {
            do {
                count = new DataTypeLong(0L, mCursor.getLong(mCursor.getColumnIndex(C_COUNT)));
            } while (mCursor.moveToNext());
        }
        if (!mCursor.isClosed()) {
            mCursor.close();
        }
        return count;
    }

    public ContentValues prepareData(int dataSourceId, DataType dataType) {
        ContentValues contentValues = new ContentValues();

        byte[] dataTypeArray = toBytes(dataType);

        contentValues.put(C_DATASOURCE_ID, dataSourceId);
        contentValues.put(C_DATETIME, dataType.getDateTime());
        contentValues.put(C_SAMPLE, dataTypeArray);
        return contentValues;
    }

    public ContentValues prepareDataHF(int dataSourceId, DataTypeDoubleArray dataType) {
        ContentValues contentValues = new ContentValues();
        byte[] dataTypeArray = dataType.toRawBytes();

        contentValues.put(C_DATASOURCE_ID, dataSourceId);
        contentValues.put(C_DATETIME, dataType.getDateTime());
        contentValues.put(C_SAMPLE, dataTypeArray);
        return contentValues;
    }

    public void commit(SQLiteDatabase db) {
        insertDB(db, TABLE_NAME, cValues);
        insertDB(db, HIGHFREQ_TABLE_NAME, hfValues);
    }

    byte[] toBytes(DataType dataType) {
        byte[] bytes;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Output output = new Output(baos);
        kryo.writeClassAndObject(output, dataType);
        output.close();
        bytes = baos.toByteArray();
        return bytes;
    }

    DataType fromBytes(byte[] bytes) {
        Input input = new Input(new ByteArrayInputStream(bytes));
        DataType dataType = (DataType) kryo.readClassAndObject(input);
        input.close();
        return dataType;
    }

}
