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

import java.util.List;

import android.content.Context;
import android.util.Log;

public class DBAdapter
{
	private Context							mContext                = null;
	private static DBAdapter				sAdapter                = null;

    private static final int 	DB_RETRY_LIMIT = 10;
	
    private static final String TAG = "Fingraph_DB";
   
    public DBAdapter( Context context )
    {
        mContext = context;
    }

    public synchronized static DBAdapter getAdapter(Context context)
    {
        if (sAdapter == null)
        {
            sAdapter = new DBAdapter(context.getApplicationContext());
        }

        return sAdapter ;
    }
    
    public void insertLog(DBRecordHolder record){
    	DBHelper helper = DBHelper.getHelper(mContext);
    	DBInsertThread insertThread = new DBInsertThread(helper, record);
    	insertThread.run();
    }
    
    public List<DBRecordHolder> loadAllLogs(){
    	DBHelper helper = DBHelper.getHelper(mContext);
    	return helper.loadAllLogs();
    }
   
    class DBInsertThread extends Thread
    {
        private DBHelper helper ;
        private DBRecordHolder record;

        DBInsertThread(DBHelper helper, DBRecordHolder record)
        {
            this.helper = helper;
            this.record = record;
        }

        @Override
        public void run()
        {
        	int retry_cnt = 0;
        	Log.i(TAG, "Writting ...");
              
        	while (retry_cnt++ < DB_RETRY_LIMIT ) {
        		try {
        			helper.insertLog(record);
        			break;
        		} catch (Exception e) {
        			Log.w("TAG", "Insert Failed! [" + retry_cnt + "]");
        		}
        	}
        }
    }
    
}
