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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import edu.tjhsst.ion.gcmFrame.com.gcmquickstart.R;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "From: " + from);

        /**
         * Production applications would usually process the message here.
         * Eg: - Syncing with server.
         *     - Store message in local database.
         *     - Update UI.
         */

        /**
         * In some cases it may be useful to show a notification indicating to the user
         * that a message was received.
         */
        sendNotification(data);
    }

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param data GCM data received.
     * title
     * text
     * url
     * sound
     * ongoing
     * wakeup
     */
    private void sendNotification(Bundle data) {
        Vibrator vib = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        int vibrate = Integer.parseInt(data.getString("vibrate", "0"));
        if (vibrate > 0) {

            vib.vibrate(vibrate);
        }
        String[] vibratepattern = data.getString("vibrate", "").split(",");
        long[] vibpattern = new long[vibratepattern.length];
        int i = 0;
        for (String p : vibratepattern) {
            vibpattern[i++] = Long.parseLong(p);
        }
        if (vibratepattern.length > 0) {
            vib.vibrate(vibpattern, -1);
        }
        String notif_title = data.getString("title", "");
        String notif_text = data.getString("text", "");
        String notif_url = data.getString("url", "");
        String notif_strsound = data.getString("sound", "");
        String notif_strongoing = data.getString("ongoing", "");
        String notif_strwakeup = data.getString("wakeup", "");
        boolean notif_sound = notif_strsound.equals("true");
        boolean notif_ongoing = notif_strongoing.equals("true");
        boolean notif_wakeup = notif_strwakeup.equals("true");

        Intent intent;
        if (notif_url.length() > 0) {
            intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        } else {
            intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(notif_url));
        }
        PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent, 0);
        PowerManager.WakeLock wl = null;
        if (notif_wakeup) {
            Log.d("showNotification", "Wakeup enabled");
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            // FIXME: FULL_WAKE_LOCK is deprecated
            wl = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Tag");
            wl.acquire(500);
        }
        // NotificationCompat supports API level 12
        NotificationCompat.Builder n = new NotificationCompat.Builder(this)
                .setContentTitle(notif_title)
                .setContentText(notif_text)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_stat_ic_notification))
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .setOngoing(notif_ongoing)
                .setTicker(notif_title + ": " + notif_text);
        if (notif_sound) {
            n.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notif = n.build();
        notificationManager.notify(0, notif);

        if (notif_wakeup) {
            wl.release();
        }

    }
}
