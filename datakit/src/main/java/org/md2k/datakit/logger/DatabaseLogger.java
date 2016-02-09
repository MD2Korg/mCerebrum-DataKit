package org.md2k.datakit.logger;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.md2k.datakit.operation.FileManager;
import org.md2k.datakitapi.datatype.DataType;
import org.md2k.datakitapi.datatype.DataTypeDoubleArray;
import org.md2k.datakitapi.datatype.RowObject;
import org.md2k.datakitapi.source.datasource.DataSource;
import org.md2k.datakitapi.source.datasource.DataSourceClient;
import org.md2k.datakitapi.status.Status;
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

    public DatabaseLogger(Context context) {
        super(context, FileManager.getFilePath(context), null, FileManager.VERSION);
        db = this.getWritableDatabase();
        Log.d(TAG, "DataBaseLogger() db isopen=" + db.isOpen() + " readonly=" + db.isReadOnly() + " isWriteAheadLoggingEnabled=" + db.isWriteAheadLoggingEnabled());
        databaseTable_dataSource = new DatabaseTable_DataSource(db);
        databaseTable_data = new DatabaseTable_Data(db);
    }

    public static DatabaseLogger getInstance(Context context) throws IOException {
        if (instance == null) {
            String directory = FileManager.getDirectory(context);
            Log.d(TAG, "directory=" + directory);
            if (directory != null)
                instance = new DatabaseLogger(context);
            else throw new IOException("Database directory not found");
        }
        return instance;
    }

    public void removeAll() {
        //TODO: need to check crashes
        databaseTable_data.removeAll(db);
        databaseTable_dataSource.removeAll(db);
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

    public Status insert(int dataSourceId, DataType dataType) {
        return databaseTable_data.insert(db, dataSourceId, dataType);
    }

    public Status insertHF(int dataSourceId, DataTypeDoubleArray dataType) {
        return databaseTable_data.insertHF(db, dataSourceId, dataType);
    }

    public ArrayList<DataType> query(int ds_id, long startTimestamp, long endTimestamp) {
        return databaseTable_data.query(db, ds_id, startTimestamp, endTimestamp);
    }

    public ArrayList<DataType> query(int ds_id, int last_n_sample) {
        return databaseTable_data.query(db, ds_id, last_n_sample);
    }

    public ArrayList<RowObject> queryLastKey(int ds_id, long last_key, int limit) {
        return databaseTable_data.queryLastKey(db, ds_id, last_key, limit);
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
}
