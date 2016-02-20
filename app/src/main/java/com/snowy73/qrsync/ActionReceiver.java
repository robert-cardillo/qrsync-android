package com.snowy73.qrsync;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class ActionReceiver extends BroadcastReceiver {
    public ActionReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String data = intent.getStringExtra(Intent.EXTRA_TEXT);
        int notificationId = intent.getIntExtra("notificationId", -1);
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        switch (intent.getAction()) {
            case "com.snowy73.qrsync.COPY":
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("QRSync", data);
                clipboard.setPrimaryClip(clip);
                Toast toast = Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT);
                toast.show();
                break;
            case "com.snowy73.qrsync.SHARE":
                Intent shareActionIntent = new Intent(Intent.ACTION_SEND);
                shareActionIntent.putExtra(Intent.EXTRA_TEXT, data);
                shareActionIntent.setType("text/plain");
                shareActionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(shareActionIntent);
                break;
            case "com.snowy73.qrsync.VIEW":
                Intent openActionIntent = new Intent(Intent.ACTION_VIEW);
                openActionIntent.setData(Uri.parse(data));
                openActionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(openActionIntent);
                break;
        }
    }
}
