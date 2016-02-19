package com.snowy73.qrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.snowy73.qrsync.barcode.BarcodeCaptureActivity;
import com.snowy73.qrsync.gcm.RegistrationIntentService;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    private BroadcastReceiver mRegistrationBroadcastReceiver;

    private TextView statusMessage;
    private ProgressBar busy;
    private Button pair;

    private static final int RC_BARCODE_CAPTURE = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        statusMessage = (TextView) findViewById(R.id.status_message);
        busy = (ProgressBar) findViewById(R.id.busy);
        pair = (Button) findViewById(R.id.pair);
        pair.setOnClickListener(this);
        if (sharedPreferences.getString(QRSyncPreferences.TOKEN, null) != null) {
            pair.setText(R.string.unpair);
            statusMessage.setText(R.string.device_is_paired);
        }

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (intent.getAction())
                {
                    case QRSyncPreferences.REGISTRATION_COMPLETE:
                        boolean sentToken = sharedPreferences
                                .getBoolean(QRSyncPreferences.SENT_TOKEN_TO_SERVER, false);
                        if (!sentToken) {
                            statusMessage.setText(R.string.token_error_message);
                        }
                        break;
                    case QRSyncPreferences.DEVICE_PAIRED:
                        statusMessage.setText(R.string.device_is_paired);
                        pair.setText(R.string.unpair);
                        break;
                    case QRSyncPreferences.DEVICE_UNPAIRED:
                        statusMessage.setText(R.string.barcode_header);
                        pair.setText(R.string.read_barcode);
                        break;
                }

            }
        };

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QRSyncPreferences.REGISTRATION_COMPLETE));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QRSyncPreferences.DEVICE_PAIRED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(QRSyncPreferences.DEVICE_UNPAIRED));
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
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Called when a view has been clicked.
     *
     * @param v The view that was clicked.
     */
    @Override
    public void onClick(View v) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (v.getId() == R.id.pair) {
            final String token = sharedPreferences.getString(QRSyncPreferences.TOKEN, null);
            if (token == null) {
                // launch barcode activity.
                Intent intent = new Intent(this, BarcodeCaptureActivity.class);
                intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
                intent.putExtra(BarcodeCaptureActivity.UseFlash, false);

                startActivityForResult(intent, RC_BARCODE_CAPTURE);
            } else {
                try {
                    busy.setVisibility(ProgressBar.VISIBLE);
                    String registration_id = sharedPreferences.getString(QRSyncPreferences.REGISTRATION_ID, null);

                    String url = MySingleton.getURL() + "/unpair";
                    JSONObject json = new JSONObject();
                    json.put("token", token);
                    json.put("registration_id", registration_id);
                    JsonObjectRequest jsObjRequest = new JsonObjectRequest
                            (Request.Method.POST, url, json, new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    try {
                                        busy.setVisibility(ProgressBar.GONE);
                                        String status = response.getString("status");
                                        if (status.equals("success")) {
                                            sharedPreferences.edit().remove(QRSyncPreferences.TOKEN).apply();
                                            Intent broadcastIntent = new Intent(QRSyncPreferences.DEVICE_UNPAIRED);
                                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                                        } else {
                                            String error = response.getString("error");
                                            statusMessage.setText(error);
                                        }

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }, new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    busy.setVisibility(ProgressBar.GONE);
                                    statusMessage.setText(error.toString());
                                }
                            });
                    MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode
     * you started it with, the resultCode it returned, and any additional
     * data from it.  The <var>resultCode</var> will be
     * {@link #RESULT_CANCELED} if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p/>
     * <p>You will receive this call immediately before onResume() when your
     * activity is re-starting.
     * <p/>
     *
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @see #startActivityForResult
     * @see #createPendingResult
     * @see #setResult(int)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    try {
                        final Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                        statusMessage.setText(R.string.pairing_device);
                        Log.d(TAG, "Barcode read: " + barcode.rawValue);

                        busy.setVisibility(ProgressBar.VISIBLE);
                        String registration_id = sharedPreferences.getString(QRSyncPreferences.REGISTRATION_ID, null);

                        String url = MySingleton.getURL() + "/pair";
                        JSONObject json = new JSONObject();
                        json.put("token", barcode.rawValue);
                        json.put("registration_id", registration_id);
                        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                                (Request.Method.POST, url, json, new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        try {
                                            busy.setVisibility(ProgressBar.GONE);
                                            String status = response.getString("status");
                                            if (status.equals("success")) {
                                                sharedPreferences.edit().putString(QRSyncPreferences.TOKEN, barcode.rawValue).apply();
                                                Intent broadcastIntent = new Intent(QRSyncPreferences.DEVICE_PAIRED);
                                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
                                            } else {
                                                String error = response.getString("error");
                                                statusMessage.setText(error);
                                            }

                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        busy.setVisibility(ProgressBar.GONE);
                                        statusMessage.setText(error.toString());
                                    }
                                });

                        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    statusMessage.setText(R.string.barcode_failure);
                    Log.d(TAG, "No barcode captured, intent data is null");
                }
            } else {
                statusMessage.setText(String.format(getString(R.string.barcode_error),
                        CommonStatusCodes.getStatusCodeString(resultCode)));
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
