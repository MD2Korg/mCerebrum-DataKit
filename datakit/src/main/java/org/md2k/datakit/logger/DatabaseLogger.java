package org.md2k.datakit.logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.md2k.datakit.Constants;
import org.md2k.datakit.configuration.Configuration;
import org.md2k.datakit.configuration.ConfigurationManager;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.DataTypeLong;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.status.Status;
import org.md2k.utilities.FileManager;
import org.md2k.utilities.Report.Log;

import java.io.IOException;
import java.util.ArrayList;

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

public class DatabaseLogger extends SQLiteOpenHelper {
    private static final String TAG = DatabaseLogger.class.getSimpleName();
    private static DatabaseLogger instance = null;
    DatabaseTable_DataSource databaseTable_dataSource = null;
    DatabaseTable_Data databaseTable_data = null;
    SQLiteDatabase db = null;

    gzipLogger gzLogger = null;

    public DatabaseLogger(Context context, String path) {
        super(context, path, null, 1);
        db = this.getWritableDatabase();
        Log.d(TAG, "DataBaseLogger() db isopen=" + db.isOpen() + " readonly=" + db.isReadOnly() + " isWriteAheadLoggingEnabled=" + db.isWriteAheadLoggingEnabled());
        databaseTable_dataSource = new DatabaseTable_DataSource(db);
        gzLogger = new gzipLogger(context);
        databaseTable_data = new DatabaseTable_Data(db, gzLogger);
    }

    public static DatabaseLogger getInstance(Context context) throws IOException {
        if (instance == null) {
            Configuration configuration=ConfigurationManager.getInstance(context).configuration;
            String directory=FileManager.getDirectory(context, configuration.database.location);
            Log.d(TAG, "directory=" + directory);
            if (directory != null)
                instance = new DatabaseLogger(context, directory+ Constants.DATABASE_FILENAME);
            else throw new IOException("Database directory not found");
        }
        return instance;
    }
    public static boolean isAlive(){
        return instance != null;
    }

    public void close() {
        if(instance!=null) {
            Log.d(TAG, "close()");
            databaseTable_data.commit(db);
            if (db.isOpen())
                db.close();
            super.close();
            db = null;
            instance = null;
        }
    }

    public Status insert(int dataSourceId, DataType[] dataType) {
        return databaseTable_data.insert(db, dataSourceId, dataType);
    }

    public Status insertHF(int dataSourceId, DataTypeDoubleArray[] dataType) {
        return databaseTable_data.insertHF(dataSourceId, dataType);
    }

    public ArrayList<DataType> query(int ds_id, long startTimestamp, long endTimestamp) {
        return databaseTable_data.query(db, ds_id, startTimestamp, endTimestamp);
    }

    public ArrayList<DataType> query(int ds_id, int last_n_sample) {
        return databaseTable_data.query(db, ds_id, last_n_sample);
    }


    public ArrayList<RowObject> queryLastKey(int ds_id, int limit) {
        return databaseTable_data.queryLastKey(db, ds_id, limit);
    }

    public ArrayList<RowObject> querySyncedData(int ds_id, long ageLimit, int limit) {
        return databaseTable_data.querySyncedData(db, ds_id, ageLimit, limit);
    }

    public boolean setSyncedBit(int ds_id, long key) {
        return databaseTable_data.setSyncedBit(db, ds_id, key);
    }

    public boolean removeSyncedData(int ds_id, long key) {
        return databaseTable_data.removeSyncedData(db, ds_id, key);
    }

    public DataTypeLong querySize() {
        return databaseTable_data.querySize(db);
    }

    public DataSourceClient register(DataSource dataSource) {
        return databaseTable_dataSource.register(db, dataSource);
    }

    public ArrayList<DataSourceClient> find(DataSource dataSource) {
        return databaseTable_dataSource.findDataSource(db, dataSource);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public DataTypeLong queryCount(int ds_id, boolean unsynced) {
        return databaseTable_data.queryCount(db, ds_id, unsynced);
    }

}
