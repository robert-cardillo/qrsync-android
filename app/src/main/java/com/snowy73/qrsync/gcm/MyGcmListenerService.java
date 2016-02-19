package com.snowy73.qrsync.gcm;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.gcm.GcmListenerService;
import com.snowy73.qrsync.MainActivity;
import com.snowy73.qrsync.QRSyncPreferences;
import com.snowy73.qrsync.R;

import java.util.Random;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        String action = data.getString("action");

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
            if (action != null) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                sharedPreferences.edit().remove(QRSyncPreferences.TOKEN).apply();
                Intent broadcastIntent = new Intent(QRSyncPreferences.DEVICE_UNPAIRED);
                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);
                sendNotification(getResources().getString(R.string.device_is_unpaired));
            } else if (message != null) {
                sendNotification(message);
            }
        }

        // [START_EXCLUDE]
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
        //  sendNotification(message);
        // [END_EXCLUDE]
    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message) {
        int notificationId = new Random().nextInt();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Intent copyActionIntent = new Intent();
        copyActionIntent.setAction("com.snowy73.qrsync.COPY");
        copyActionIntent.putExtra(Intent.EXTRA_TEXT, message);
        PendingIntent copyActionPendingIntent = PendingIntent.getBroadcast(this, 0, copyActionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent shareActionIntent = new Intent();
        shareActionIntent.setAction(Intent.ACTION_SEND);
        shareActionIntent.putExtra(Intent.EXTRA_TEXT, message);
        shareActionIntent.setType("text/plain");
        PendingIntent shareActionPendingIntent = PendingIntent.getActivity(this, 0, shareActionIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent openActionIntent = null;
        PendingIntent openActionPendingIntent = null;
        if (message.startsWith("http://") || message.startsWith("https://")) {
            openActionIntent = new Intent();
            openActionIntent.setAction(Intent.ACTION_VIEW);
            openActionIntent.setData(Uri.parse(message));
            openActionPendingIntent = PendingIntent.getActivity(this, 0, openActionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_stat_ic_notification)
                .setContentTitle("QRSync")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_save, "Copy", copyActionPendingIntent)
                .addAction(android.R.drawable.ic_menu_share, "Share", shareActionPendingIntent);
        if (openActionIntent != null) {
            notificationBuilder.addAction(android.R.drawable.ic_menu_send, "Open URL", openActionPendingIntent);
        }

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notificationBuilder.build());
    }
}
