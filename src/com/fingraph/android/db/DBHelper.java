/*******************************************************************************
 * Copyright 2014 tgrape Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.fingraph.android.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper
{	
	public static final String 	DB_NAME	                    = "fingraph.db" ;
    public static final int    	DB_VERSION                 	= 1;
    private static final int 	DB_BATCH_SELECT_LIMIT		= 10;
    
    public static final String TABLE_NAME					= "fingraph_table" ;   // TABLE name
    public static final String FIELD_ID						= "_id" ;              // row index
    public static final String FIELD_COMMAND				= "command" ;          // COMMAND 정보
    public static final String FIELD_SEND_DATA				= "send_data" ;        // 보낼 데이터 정보
    public static final String FIELD_DATE					= "date" ;             // 저장된 날자
   
    private static DBHelper sHelper;
   
    public DBHelper( Context _context )
    {
        super ( _context, DB_NAME, null, DB_VERSION );
    }
   
    public synchronized static DBHelper getHelper(Context context)
    {
        if (sHelper == null)
        {
            sHelper = new DBHelper(context.getApplicationContext());
        }

        return sHelper ;
    }

    public synchronized static void closeHelper()
    {
        if (sHelper != null)
        {
            sHelper .close();
            sHelper = null ;
        }
    }

    @Override
    public void onCreate( SQLiteDatabase _db )
    {
        _db.execSQL
        (
            "CREATE TABLE "     + TABLE_NAME
            + " ( "
            + FIELD_ID          + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + FIELD_COMMAND     + " TEXT,"
            + FIELD_SEND_DATA   + " TEXT,"
            + FIELD_DATE        + " TEXT"
            + " );"
        );
       
    }

    @Override
    public void onUpgrade( SQLiteDatabase _db,
                           int _oldVersion,
                           int _newVersion )
    {

              _db.execSQL( "drop table session" );
            onCreate(_db);
    }
   
    public void insertLog( DBRecordHolder record)
    {
        final SQLiteDatabase writableDatabase = getWritableDatabase();
        final ContentValues values = new ContentValues();
       
        values.put( DBHelper.FIELD_COMMAND , record.command );
        values.put( DBHelper.FIELD_SEND_DATA , record.sendData );
        values.put( DBHelper.FIELD_DATE , record.date );

        writableDatabase.insertOrThrow(DBHelper.TABLE_NAME , null , values);
    }
    
    public List<DBRecordHolder> loadAllLogs()
    {
    	final SQLiteDatabase writableDatabase = getWritableDatabase();
        List<DBRecordHolder> logRecords;
        
        StringBuilder buff = new StringBuilder();
        buff.append( "SELECT * FROM " + DBHelper.TABLE_NAME);
        // DB에 저장되었던 순서대로 재발송을 위해 순차정렬
        buff.append( " ORDER BY _id ASC" );
        // 결과는 10개로 제한
        buff.append( " LIMIT " + DB_BATCH_SELECT_LIMIT);        
   
        writableDatabase.beginTransaction();
        try {
        	final Cursor recordCursor = writableDatabase.rawQuery( buff.toString(), null );
        	
        	logRecords = new ArrayList<DBRecordHolder>();
        	
        	while (recordCursor.moveToNext())
            {
        		final DBRecordHolder record = logRecordFromCursor(recordCursor);

        		long id = recordCursor.getLong(recordCursor.getColumnIndex("_id"));
        		
        		writableDatabase.delete(DBHelper.TABLE_NAME, "_id = " + id, null);
        		
                logRecords.add(record);
            }
        	
        	recordCursor.close();
        	
        	writableDatabase.setTransactionSuccessful();
        } catch (Exception e) {
        	logRecords = null;
        } finally {
        	writableDatabase.endTransaction();
        }
        
        return logRecords;
    }
    
    private DBRecordHolder logRecordFromCursor(Cursor cursor)
    {
        final DBRecordHolder record = new DBRecordHolder(cursor.getString(1), cursor.getString(2), cursor.getString(3));
        return record;
    }
   
}
