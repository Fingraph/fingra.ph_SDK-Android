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
package com.fingraph.android.manager;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;

import com.fingraph.android.db.DBAdapter;
import com.fingraph.android.db.DBRecordHolder;
import com.fingraph.android.utils.Const;

public class SendManager
{

    private static SendManager	mInstance        = null;
    private SessionSendTask		mSessionSendTask = null;
    private Context				mContext         = null;
    
    public static final int		TIMEOUT_SEC      = 10000;
    public static final int		RETRY_DELAY_SEC  = 2000;
    public static final int		RETRY_LIMIT		 = 1;

    public SendManager( Context _context )
    {
        mContext = _context;
    }

    public static SendManager getInstance( Context _context )
    {
        if ( mInstance == null )
        {
            mInstance = new SendManager( _context );
        }
        
        return mInstance;
    }

    /**
     * Fingraph Sever로 데이터 전송
     * 
     * @param _url
     * @param _data
     * @return
     */
    private boolean requestToServer( String _data )
    {

        String url = Const.FINGRAPHAGENT_SERVER_URL;

        boolean result = false;
        HttpURLConnection conn = null;
        OutputStream os = null;
        
        int networkErrCount = 0;
        
        do
        {
            try
            {
                // 재시도 횟수가 (1회) 넘으면 멈춘다.
                if ( networkErrCount > RETRY_LIMIT ) break;
                
                if ( networkErrCount > 0 )
                {
                    Thread.sleep( RETRY_DELAY_SEC );
                }
                
                URL connectUrl = new URL( url );

                conn = (HttpURLConnection)connectUrl.openConnection();
                conn.setConnectTimeout( TIMEOUT_SEC );
                conn.setReadTimeout( TIMEOUT_SEC );
                conn.setRequestMethod( "POST" );
                conn.setRequestProperty( "Cache-Control", "no-cache" );
                conn.setRequestProperty( "Content-Type", "application/json" );
                conn.setRequestProperty( "Accept", "application/json" );
                conn.setDoOutput( true );
                conn.setDoInput( true );

                os = conn.getOutputStream();
                os.write( _data.getBytes() );
                os.flush();

                int responseCode = conn.getResponseCode();

                if ( responseCode == HttpURLConnection.HTTP_OK )
                {
                    result = true;
                    break;
                }
                else
                {
                    networkErrCount++;
                }

            }
            catch ( Exception e )
            {
                e.printStackTrace();
                networkErrCount++;
            }
        }
        while ( true );

        return result;

    }
    
    /**
     * SessionSendTask(Default)
     * 
     * @param _sendData
     * @param _cmd
     */
    public void executeSessionSendTask( DBRecordHolder _record )
    {
    	
    	mSessionSendTask = new SessionSendTask( _record, false);
        
        try
        {
            mSessionSendTask.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    /**
     * SessionSendTask(Batch Mode)
     * 
     * @param _record
     * @param _isBatch
     */
    public void executeSessionSendTask(DBRecordHolder _record, boolean _isBatch)
    {
    	
    	mSessionSendTask = new SessionSendTask( _record, _isBatch);
        
        try
        {
            mSessionSendTask.execute();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }

    private class SessionSendTask extends AsyncTask<String, Void, Boolean>
    {
        DBRecordHolder record = null;
        boolean isBatch = false;

        public SessionSendTask( DBRecordHolder _record, boolean _isBatch)
        {
            record = _record;
            isBatch = _isBatch;
        }

        protected void onPreExecute()
        {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground( String... params )
        {
        	boolean result = false;
        	
            try
            {
                result = requestToServer( record.getSendData() );
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }

            return result;
        }

        protected void onPostExecute( Boolean _result )
        {
            super.onPostExecute( _result );
            
            if ( _result == true )
            {
            	// 전송 시도 성공 후 DB에 저장된 전송실패 로그가 있는지 확인 후 재전송을 요청한다.
            	DBAdapter dbAdapter = DBAdapter.getAdapter(mContext);
            	List<DBRecordHolder> logRecords = dbAdapter.loadAllLogs();
            	
            	// 저장된 (전송 실패) 로그들을 재전송 - Batch(Json Array로) 재전송 가능하도록 보완 필요
            	for (DBRecordHolder record : logRecords) {
            		executeSessionSendTask (record);
            	}
            }
            else  
            {
            	// 전송이 시도 완료 후 실패시 DB에 기록한다.
                if (isBatch) {
               		//insertBatch(data);
               		
               	} else {
               		DBAdapter dbAdapter = DBAdapter.getAdapter(mContext);
               		dbAdapter.insertLog(record);
               	}
            }
        }
    }

}
