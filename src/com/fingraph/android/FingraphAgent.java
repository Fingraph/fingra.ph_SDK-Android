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
package com.fingraph.android;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import org.json.JSONObject;

import com.fingraph.android.db.DBRecordHolder;
import com.fingraph.android.manager.DeviceInfo;
import com.fingraph.android.manager.SendManager;
import com.fingraph.android.utils.Const;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

public class FingraphAgent
{
    public static final String       TAG             = "fingraph";

    private static FingraphAgent     sInstance;
    private SharedPreferences        mPrefs          = null;
    private SharedPreferences.Editor mEditor         = null;
    private static Context           mContext        = null;
    private SendManager              mSendManager    = null;

    private String                   mAppKey         = null;
    private String                   mRefKey         = null;
    private String                   mSessionId      = "";
    
    private int                      mSessionTimeOut = 10000;
    
    static public FingraphAgent getInstance( Context _context )
    {
        if ( sInstance == null )
        {
            sInstance = new FingraphAgent( _context );
        }

        return sInstance;
    }

    private FingraphAgent( Context _context )
    {

        mContext = _context;

        mSendManager = new SendManager( mContext );

        mPrefs = mContext.getSharedPreferences( Const.PREFERENCE_NAME,
                                                Context.MODE_PRIVATE );
        mEditor = mPrefs.edit();

        DeviceInfo.setDeviceInfo( mContext );

        mAppKey = getAppKey( mContext );

    }
    
    /**
     * ContinueSessionTime 설정 기본 10초
     * 
     * @param _time
     */
    public void onContinueSession( int _time )
    {
        mSessionTimeOut = _time;
    }

    public synchronized void onActivityStart()
    {
        onStartSession();
    }
    
    /**
     * SessionID가 null이거나 빈공백이면 SessionId를 생성후 서버로 전송
     */
    private synchronized void onStartSession()
    {

        String prefPauseTime = mPrefs.getString( Const.PREFS_PAUSE_TIME, "" );
        // ====================================================================
        // SharedPreferences 저장된 PauseTime이 0보다 크면 
        // 현재 시간을 가져와 PauseTime과 비교해서 ContinueSessionTime(기본10초)
        // 을 지나면 mSessionId를 초기화 해준다.
        // ====================================================================
        if( mSessionId != null && false == "".equals( prefPauseTime ) )
        {
            long pauseTime = Long.parseLong( prefPauseTime );
            
            if( pauseTime >  0 )
            {
                //현재 시간을 가져온다.
                long nowTime = System.currentTimeMillis();
                
                // 현재시간 - 저장된 pausetime > 설정된 ContinueSessionTime(10초)
                if ( ( nowTime - pauseTime ) > mSessionTimeOut )
                {
                    mSessionId = null;
                }
            }
        }
        // ====================================================================

        if ( mSessionId == null || true == "".equals( mSessionId ) )
        {
            String sendData = "";
            
            // ============================================================
            // 1. SessionId를 생성해준다.
            // ============================================================
            mSessionId = getSessionID();
            // ============================================================

            // ============================================================
            // 2. SessionID를 SharedPreferences에 저장해준다.
            // ============================================================
            mEditor.putString( Const.PREFS_SESSION_ID, mSessionId );
            mEditor.commit();
            // ============================================================

            // ============================================================
            // 4. Fingraph 서버로 전송할 데이터를 생성한다.
            // ============================================================
            sendData = setSendData( Const.START_SESSION_COMMAND, null, null );
            
            DBRecordHolder record = new DBRecordHolder( Const.START_SESSION_COMMAND, sendData, getLocalTime());
            // ============================================================

            // ============================================================
            // 5. Fingraph 서버로 데이터 전송, 실패시 DB에 저장
            // ============================================================
            mSendManager.executeSessionSendTask( record );
            // ============================================================
        }
    }

    public void onPageView()
    {
        String sendData = "";
        if ( mSessionId != null )
        {
            // ================================================================
            // 1. Fingraph Server에 보낼 데이터를 세팅한다.
            // ================================================================
            sendData = setSendData( Const.PAGEVIEW_COMMAND, null, null );
            
            DBRecordHolder record = new DBRecordHolder( Const.PAGEVIEW_COMMAND, sendData, getLocalTime());
            // ================================================================

            // ================================================================
            // 2. Fingraph 서버로 데이터 전송, 실패시 DB에 저장
            // ================================================================
            mSendManager.executeSessionSendTask( record );
            // ================================================================
        }

    }

    public void onEvent( String _eventKey )
    {
        String sendData = "";
        
    	// Session ID가 없을 경우 onStartSession()이 호출되지 않은 것으로 간주하여 세션 ID 생성 후 전송
    	if ( mSessionId == null ) onStartSession();
        
    	// ================================================================
    	// 1. Fingraph Server에 보낼 데이터를 세팅한다.
    	// ================================================================
    	sendData = setSendData( Const.EVENT_COMMAND, _eventKey, null );
    	
    	DBRecordHolder record = new DBRecordHolder( Const.EVENT_COMMAND, sendData, getLocalTime());
    	// ================================================================

	    // ================================================================
        // 2. Fingraph 서버로 데이터 전송, 실패시 DB에 저장
    	// ================================================================
    	mSendManager.executeSessionSendTask( record );
    	// ================================================================
    }

    public synchronized void onActivityStop()
    {
        onEndSession();
    }

    /**
     * 앱이 백그라운드 상태인지 체크
     * 
     * @param _context
     * @return
     */
    public static boolean isApplicationInBackground()
    {
        ActivityManager am = (ActivityManager)mContext.getSystemService( Context.ACTIVITY_SERVICE );

        List<RunningTaskInfo> tasks = am.getRunningTasks( 1 );

        if ( !tasks.isEmpty() )
        {
            ComponentName topActivity = tasks.get( 0 ).topActivity;

            if ( !topActivity.getPackageName().equals( mContext.getPackageName() ) )
            {
                return true;
            }
        }

        return false;
    }

    private synchronized void onEndSession()
    {

    	// Session ID가 없을 경우 onStartSession()이 호출되지 않은 것으로 간주하여 세션 ID 생성 후 전송
    	if ( mSessionId == null ) onStartSession();
    	
    	// ========================================================
    	// 1.현재 시스템 시간을 저장
    	// ========================================================
    	long pauseTime = System.currentTimeMillis();
        	
    	// PauseTime 저장
    	mEditor.putString( Const.PREFS_PAUSE_TIME, 
        			Long.toString( pauseTime ));
    	mEditor.commit();

    	String sendData = setSendData( Const.END_SESSION_COMMAND,
        			null, null );                    
    	
    	DBRecordHolder record = new DBRecordHolder( Const.END_SESSION_COMMAND, sendData, getLocalTime());
    	// ========================================================
            
    	//  2. Fingraph 서버로 데이터 전송, 실패시 DB에 저장
    	mSendManager.executeSessionSendTask( record );
    	// ========================================================
    }

    public String setSendData( String cmd,
                               String event_key,
                               String trace_info )
    {

        if ( mSessionId == null )
        {
            return null;

        }
        
        
        JSONObject jsonData = new JSONObject();
        try
        {
            // ================================================================
            // 모든 COMMAND 공통 사항
            // ================================================================
            jsonData.put( "cmd", cmd );
            jsonData.put( "session", mSessionId );
            jsonData.put( "utctime", getUTCTime() );
            jsonData.put( "localtime", getLocalTime() );
            jsonData.put( "token", DeviceInfo.mUdid );
            jsonData.put( "device", DeviceInfo.mModel );
            jsonData.put( "osversion", DeviceInfo.mOsVersion );
            jsonData.put( "resolution", DeviceInfo.mResolution );
            jsonData.put( "appversion", DeviceInfo.mAppVersion );
            jsonData.put( "language", DeviceInfo.mLanguage.toLowerCase(Locale.getDefault()));
            jsonData.put( "country", DeviceInfo.mCountry.toUpperCase(Locale.getDefault()));
            jsonData.put( "appkey", mAppKey );
            // ================================================================

            
            // cmd가 3EVENT이고 키값이 있을때만
            if( true == Const.EVENT_COMMAND.equals( cmd ) )
            {
                if ( event_key != null && event_key.length() > 0 )
                {
                    jsonData.put( "eventkey", event_key );
                }
            }
            
            jsonData.put( "referrerkey", mRefKey );
        }
        catch( Exception e)
        {
            e.printStackTrace();
        }

        Log.v(TAG,"jsonData         "+jsonData.toString());
        
        return jsonData.toString();

    }

    private String getSessionID()
    {

        return UUID.randomUUID().toString();

    }
    
    private String getAppKey( Context _context )
    {

        String app_key = "";

        try
        {

            ApplicationInfo ai = _context.getPackageManager().getApplicationInfo( _context.getPackageName(),
                                                                             PackageManager.GET_META_DATA );

            Bundle bundle = ai.metaData;

            if ( bundle != null )
            {
                app_key = bundle.getString( "fingraph_app_key" );
            }

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return app_key;

    }

    private String getLocalTime()
    {

        return getLocalTime( new Date().getTime(), null );

    }

    //@Param StringBuilder - Call By Reference
    private String getLocalTime( long m, StringBuilder expiredTime )
    {
        Date d = new java.util.Date();
        d.setTime( m );

        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat( "yyyyMMddHHmmss", Locale.ENGLISH );
        String localtime = formatter.format( d );
        
    	if (expiredTime != null) {
    		d.setTime(m - (SendManager.TIMEOUT_SEC + SendManager.RETRY_DELAY_SEC) * SendManager.RETRY_LIMIT);
            expiredTime.delete(0, expiredTime.length());
            expiredTime.append(formatter.format( d ));
    	}
        
        return localtime;
    }

    private String getUTCTime()
    {
    	
        return getUTCTime( new Date().getTime() );

    }

    private String getUTCTime( long longLocalTime )
    {

        java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat( "yyyyMMddHHmmss", Locale.ENGLISH );

        TimeZone zone = TimeZone.getDefault();
        int offset = zone.getOffset( longLocalTime );
        long longUTCTime = longLocalTime - offset;

        Date dateUTCTime = new Date();
        dateUTCTime.setTime( longUTCTime );

        return formatter.format( longUTCTime );

    }
    
    
   
    /**
     * 두 날자 비교
     * @param _today
     * @param _oldDay
     * @return
     */
    public static int getDifferenceOfDate( String _today, String _oldDay)
    {
        int nYear1 = Integer.parseInt( _today.substring( 0, 4 ) );
        int nMonth1 = Integer.parseInt( _today.substring( 4, 6 ) );
        int nDate1 = Integer.parseInt( _today.substring( 6, 8 ) );

        int nYear2 = Integer.parseInt( _oldDay.substring( 0, 4 ) );
        int nMonth2 = Integer.parseInt( _oldDay.substring( 4, 6 ) );
        int nDate2 = Integer.parseInt( _oldDay.substring( 6, 8 ) );
        Calendar cal = Calendar.getInstance();
        int nTotalDate1 = 0, nTotalDate2 = 0, nDiffOfYear = 0, nDiffOfDay = 0;

        if ( nYear1 > nYear2 )
        {
            for ( int i = nYear2 ; i < nYear1 ; i++ )
            {
                cal.set( i, 12, 0 );
                nDiffOfYear += cal.get( Calendar.DAY_OF_YEAR );
            }
            nTotalDate1 += nDiffOfYear;
        }
        else if ( nYear1 < nYear2 )
        {
            for ( int i = nYear1 ; i < nYear2 ; i++ )
            {
                cal.set( i, 12, 0 );
                nDiffOfYear += cal.get( Calendar.DAY_OF_YEAR );
            }
            nTotalDate2 += nDiffOfYear;
        }

        cal.set( nYear1, nMonth1 - 1, nDate1 );
        nDiffOfDay = cal.get( Calendar.DAY_OF_YEAR );
        nTotalDate1 += nDiffOfDay;

        cal.set( nYear2, nMonth2 - 1, nDate2 );
        nDiffOfDay = cal.get( Calendar.DAY_OF_YEAR );
        nTotalDate2 += nDiffOfDay;

        return nTotalDate1 - nTotalDate2;
    }
    
}
