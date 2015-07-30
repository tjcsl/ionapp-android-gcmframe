package edu.tjhsst.ion.gcmFrame;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.webkit.JavascriptInterface;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import edu.tjhsst.ion.gcmFrame.com.gcmquickstart.R;

public class WebAppInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public String isIonSetup() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean sentToken = sharedPreferences
                .getBoolean(QuickstartPreferences.ION_SETUP, false);
        return sentToken ? "true" : "false";
    }

    @JavascriptInterface
    public void gcmSetup(String user_token) {
        InstanceID instanceID = InstanceID.getInstance(mContext);
        try {
            String gcm_token = instanceID.getToken(mContext.getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            MainActivity.notifSetup(user_token, gcm_token, mContext);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}