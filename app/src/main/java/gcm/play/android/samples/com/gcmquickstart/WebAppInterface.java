package gcm.play.android.samples.com.gcmquickstart;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

public class WebAppInterface {
    Context mContext;

    /** Instantiate the interface and set the context */
    WebAppInterface(Context c) {
        mContext = c;
    }

    /** Show a toast from the web page */
    @JavascriptInterface
    public void gcmSetup(String user_token) {
        InstanceID instanceID = InstanceID.getInstance(mContext);
        try {
            String gcm_token = instanceID.getToken(mContext.getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            Toast.makeText(mContext, user_token+": "+gcm_token, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}