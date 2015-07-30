/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.tjhsst.ion.gcmFrame;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.tjhsst.ion.gcmFrame.com.gcmquickstart.R;

public class MainActivity extends AppCompatActivity {

    static final String ION_HOST = "https://ion.tjhsst.edu/";
    static final String ION_SETUP_URL = "https://ion.tjhsst.edu/notifications/android/setup";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private WebView webView;

    private boolean isConnected = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        this.setContentView(R.layout.activity_main);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.navbar_color));

        }
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        //mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    //mInformationTextView.setText(getString(R.string.gcm_send_message));
                    //Toast.makeText(getApplicationContext(), getString(R.string.gcm_send_message), 500).show();
                    Log.i(TAG, "Google token sent");
                    /*Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse("https://ion.tjhsst.edu/"));
                    startActivity(i);*/

                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.token_error_message), 5000).show();
                }
            }


        };


        //mInformationTextView = (TextView) findViewById(R.id.informationTextView);

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            if(ni != null) {
                NetworkInfo.State state = ni.getState();
                if (state == null || state != NetworkInfo.State.CONNECTED) {
                    // record the fact that there is not connection
                    isConnected = false;
                }
            }
        }

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean sentToken = sharedPreferences
                .getBoolean(QuickstartPreferences.ION_SETUP, false);

        webView = (WebView) findViewById(R.id.webview);

        webView.setInitialScale(1);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);

        webView.getSettings().setTextSize(WebSettings.TextSize.NORMAL);
        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webView.setWebContentsDebuggingEnabled(true);
        }
        String uaString = webView.getSettings().getUserAgentString();
        uaString += " - IonAndroid: gcmFrame (";
        if(sentToken) {
            uaString += "appRegistered:True";
        } else {
            uaString += "appRegistered:False";
        }
        uaString += " osVersion:" + System.getProperty("os.version");
        uaString += " apiLevel:" + android.os.Build.VERSION.SDK;
        uaString += " Device:" + android.os.Build.DEVICE;
        uaString += " Model:" + android.os.Build.MODEL;
        uaString += " Product:" + android.os.Build.PRODUCT;
        uaString += ")";
        webView.getSettings().setUserAgentString(uaString);

        webView.setNetworkAvailable(isConnected);


        webView.addJavascriptInterface(new WebAppInterface(this), "IonAndroidInterface");

        webView.loadUrl(MainActivity.ION_HOST);

        final String offlineHTML = "<script>url=\"[url]\"</script><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"/><body><h2>Network Disconnected</h2><h3>Unable to contact the Intranet application at this time. Try again later.</h3><button onclick='location.href.replace(url)' style='width:100%;height:50px'>Try Again</button></body>";
        final String timeoutHTML = "<script>url=\"[url]\"</script><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"/><body><h2>Error Loading Page</h2><h3>Unable to contact the Intranet application at this time. Try again later.</h3><h3>Unable to load:<br /><a href=\"[url]\">[url]</a><br /><br />[desc]</h3><button onclick='location.href.replace(url)' style='width:100%;height:50px'>Try Again</button></body>";
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Loading "+url);
                if(!isConnected) {
                    view.loadData(offlineHTML, "text/html", "utf-8");
                    return true;
                } else if(url.contains(ION_HOST)) {
                    // keep in webview
                    webView.loadUrl(url);
                    return true;
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url));
                    startActivity(intent);
                    return true;
                }
            }

            @Override
            public void onReceivedError (WebView view, int errorCode,
                                         String description, String failingUrl) {
                //if (errorCode == ERROR_TIMEOUT) {
                    view.stopLoading();  // may not be needed
                    String html = timeoutHTML;
                    html = html.replace("[url]", failingUrl);
                    html = html.replace("[url]", failingUrl);
                    html = html.replace("[url]", failingUrl);
                    html = html.replace("[desc]", description);
                    view.loadData(html, "text/html", "utf-8");
                //}
            }
        });

    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    public static void notifSetup(String user_token, String gcm_token, Context mContext) {

        Log.i(TAG, user_token+"\n"+gcm_token);
        Log.i(TAG, "Setting up notification support..");
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(ION_SETUP_URL);

        try {
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("user_token", user_token));
            nameValuePairs.add(new BasicNameValuePair("gcm_token", gcm_token));
            httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

            // Execute HTTP Post Request
            HttpResponse response = httpclient.execute(httppost);
            HttpEntity entity = response.getEntity();
            String resp = "";
            if(entity != null){
                resp = EntityUtils.toString(entity);
            }

            if(resp.indexOf("Now registered") != -1) {

                Toast.makeText(mContext, "Your device can now receive notifications from Intranet. To change this, hit the right-side user icon and tap Preferences.", Toast.LENGTH_LONG).show();
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(mContext);
                sharedPreferences.edit().putBoolean(QuickstartPreferences.ION_SETUP, true).apply();
            } else {
                Toast.makeText(mContext, "An error occurred trying to set up notifications: " + resp, Toast.LENGTH_LONG).show();
            }




        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
        } catch (IOException e) {
            // TODO Auto-generated catch block
        }
    }

}
