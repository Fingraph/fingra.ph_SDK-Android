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

import java.util.Locale;
import java.util.UUID;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public class DeviceInfo
{
    private static DeviceInfo   mInstance   = new DeviceInfo();

    private static final String BUGGY_ANDROID_SECURE_ID_ON_FROYO = "9774d56d682e549c";
    
    public static String        mUdid       = "";
    public static String        mLanguage   = "";
    public static String        mCountry    = "";
    public static String        mAppVersion = "";
    public static String        mOsVersion  = "";
    public static String        mResolution = "";
    public static String        mModel      = "";

    public static boolean       mIsLoaded   = false;

    public static Context       mContext    = null;

    private DeviceInfo()
    {

    }

    public static DeviceInfo getInstance()
    {
        return mInstance;
    }

    public static void setDeviceInfo( Context context )
    {

        if ( mIsLoaded == true )
        {
            return;
        }

        mContext = context;

        Locale lc = context.getResources().getConfiguration().locale;

        try
        {

            mUdid = GetDevicesUUID();

            if ( lc != null )
            {
                mCountry = lc.getCountry();
                Log.d("Country", mCountry);
            }

            if ( lc != null )
            {
                mLanguage = lc.getLanguage();
            }

            mAppVersion = mInstance.getAppVersion( mContext );

            mOsVersion = mInstance.getOSVersion();

            WindowManager wm = (WindowManager)mContext.getSystemService( Context.WINDOW_SERVICE );
            Display d = wm.getDefaultDisplay();

            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
                mResolution = mInstance.getResolution13(d);
            } else {
                mResolution = mInstance.getResolution(d);
            }

            mModel = Build.MODEL;

            mIsLoaded = true;

        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }
    
    
    /**
     * Device Token값 생성
     * @return
     */
    public static String GetDevicesUUID()
    {

        final TelephonyManager tm = (TelephonyManager) 
                          mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final String tmDevice, androidId;

        if (tm == null) {
        	tmDevice = "";
        } else {
            tmDevice = "" + tm.getDeviceId();
        }
        
        androidId = "" + android.provider.Settings.Secure.getString(
                                   mContext.getContentResolver(), 
                                   android.provider.Settings.Secure.ANDROID_ID);
        
        UUID deviceUuid = new UUID((long)tmDevice.hashCode(), androidId.hashCode());

    	String deviceId = deviceUuid.toString();
    
        //If Secure.ANDROID_ID has a bug or return null, it will be replaced with an alternative identifier.
        if("".equals(tmDevice) & (BUGGY_ANDROID_SECURE_ID_ON_FROYO.equals(androidId) | "".equals(androidId))){
        	deviceId = getUniquePsuedoID();
        }
        	
        return deviceId;

    }   


    /**
     * App 버전
     * @param _context
     * @return
     */
    private String getAppVersion( Context _context )
    {

        String appVersion = "";

        try
        {
            appVersion = _context.getPackageManager().getPackageInfo( _context.getPackageName(),
                                                                 0 ).versionName;

            if ( appVersion == null )
            {
                appVersion = "";
            }

            if ( appVersion.length() > 100 )
            {
                appVersion = appVersion.substring( 0, 100 );
            }

        }
        catch ( Exception e )
        {
            // Log.d("fingraph", e.toString());
            e.printStackTrace();
        }

        return appVersion;

    }

    
    /**
     * Android OS Version
     * @return
     */
    private String getOSVersion()
    {

        String osVersion = "";
        try
        {
            osVersion = android.os.Build.VERSION.RELEASE;
            if ( osVersion == null )
            {
                osVersion = "";
            }

            if ( osVersion.length() > 100 )
            {
                osVersion = osVersion.substring( 0, 100 );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            // Log.d("fingraph", e.toString());
        }

        return osVersion;
    }
    
    /**
     * Get a device resolution(HONEYCOMB_MR1 or lower)
     * @param Display d
     * @return
     */
    @SuppressWarnings("deprecation" )
    private String getResolution(Display d){
    	String r;
          
    	if(d.getWidth()<d. getHeight()) {
    		r = d. getWidth() + "X" + d.getHeight();
    	} else {
    		r = d. getHeight() + "X" + d.getWidth();
    	}
    	
    	return r;
    }
   
    /**
     * Get a device resolution(HONEYCOMB_MR2 or higher)
     * @param Display d
     * @return
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private String getResolution13(Display d){
    	String r;
          
    	Point pt = new Point();
    	d.getSize(pt);
      
    	if(pt.x < pt.y) {
    		r = pt. x + "X" + pt.y ;
    	} else {
    		r = pt. y + "X" + pt.x ;
    	}
    	
    	return r;
    }

    /**
     * Return pseudo unique ID
     * @return ID
     */
    public static String getUniquePsuedoID()
    {
        // http://www.pocketmagic.net/?p=1662!
        String m_szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10) + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10) + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10) + (Build.PRODUCT.length() % 10);

        // http://developer.android.com/reference/android/os/Build.html#SERIAL
        String serial = null;
        try
        {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();

            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        }
        catch (Exception e)
        {
            // String needs to be initialized
            serial = "UNSPECIFIED"; // some value
        }

        // http://stackoverflow.com/a/2853253/950427
        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }
    
}
