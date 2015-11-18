/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import edu.tjhsst.ion.gcmFrame.com.gcmquickstart.R;

public class MainActivity extends AppCompatActivity {

    private static final String ION_HOST = "https://ion.tjhsst.edu/";
    private static final String ION_SETUP_URL = "https://ion.tjhsst.edu/notifications/android/setup";

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private WebView webView;

    private boolean isConnected = true;

    public static void notifSetup(String user_token, String gcm_token, Context mContext) {

        Log.i(TAG, user_token + "\n" + gcm_token);
        Log.i(TAG, "Setting up notification support..");
        try {
            URL url = new URL(ION_SETUP_URL);
            Uri.Builder builder = new Uri.Builder()
                    .appendQueryParameter("user_token", user_token)
                    .appendQueryParameter("gcm_token", gcm_token);
            byte[] query = builder.build().getEncodedQuery().getBytes();
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setDoOutput(true);
            urlConnection.setFixedLengthStreamingMode(query.length);
            urlConnection.getOutputStream().write(query);
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String resp = in.readLine();
            if (resp.contains("Now registered")) {

                Toast.makeText(mContext, "Your device can now receive notifications from Intranet." +
                        "To change this, hit the right-side user icon and tap Preferences.", Toast.LENGTH_LONG).show();
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(mContext);
                sharedPreferences.edit().putBoolean(QuickstartPreferences.ION_SETUP, true).apply();
            } else {
                Toast.makeText(mContext, "An error occurred trying to set up notifications: " + resp, Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Setting up notifications failed", e);

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        this.setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.navbar_color));

        }
        // requestWindowFeature(Window.FEATURE_NO_TITLE);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    Log.i(TAG, "Google token sent");

                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.token_error_message), Toast.LENGTH_LONG).show();
                }
            }


        };


        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
            if (ni != null) {
                NetworkInfo.State state = ni.getState();
                if (state == null || state != NetworkInfo.State.CONNECTED) {
                    // record the fact that there is no connection
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

        webView.getSettings().setUseWideViewPort(true);
        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        StringBuilder uaString = new StringBuilder(webView.getSettings().getUserAgentString());
        uaString.append(" - IonAndroid: gcmFrame (");
        if (sentToken) {
            uaString.append("appRegistered:True");
        } else {
            uaString.append("appRegistered:False");
        }
        uaString.append(" osVersion:").append(System.getProperty("os.version"));
        uaString.append(" apiLevel:").append(android.os.Build.VERSION.SDK_INT);
        uaString.append(" Device:").append(android.os.Build.DEVICE);
        uaString.append(" Model:").append(android.os.Build.MODEL);
        uaString.append(" Product:").append(android.os.Build.PRODUCT);
        uaString.append(")");
        webView.getSettings().setUserAgentString(uaString.toString());

        webView.setNetworkAvailable(isConnected);


        webView.addJavascriptInterface(new WebAppInterface(this), "IonAndroidInterface");

        webView.loadUrl(MainActivity.ION_HOST);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "Loading " + url);
                if (!isConnected) {
                    String html = getHtml("offline.html");
                    html = html.replaceAll("\\[url\\]", url);
                    view.loadData(html, "text/html", "utf-8");
                    return true;
                } else if (url.contains(ION_HOST)) {
                    // keep in WebView
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
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                // if (errorCode == ERROR_TIMEOUT)
                view.stopLoading();  // may not be needed
                String html = getHtml("timeout.html");
                html = html.replaceAll("\\[url\\]", failingUrl);
                html = html.replaceAll("\\[desc\\]", description);
                view.loadData(html, "text/html", "utf-8");
            }
        });

    }


    private String getHtml(String file) {
        String html = "";
        try {
            InputStream in = getAssets().open(file);
            byte[] data = new byte[in.available()];
            in.read(data);
            html = new String(data);

        } catch (IOException e) {
            Log.e(TAG, "Failed to read HTML asset", e);
        }
        return html;
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

}
