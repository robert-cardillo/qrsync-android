package com.snowy73.qrsync;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class CopyReceiver extends BroadcastReceiver {
    public CopyReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String copy = intent.getStringExtra(Intent.EXTRA_TEXT);
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QRSync", copy);
        clipboard.setPrimaryClip(clip);
        Toast toast = Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT);
        toast.show();
    }
}
