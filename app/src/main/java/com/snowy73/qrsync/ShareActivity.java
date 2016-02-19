package com.snowy73.qrsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class ShareActivity extends AppCompatActivity {
    public ShareActivity() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Intent intent = getIntent();
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
            String token = sharedPreferences.getString(QRSyncPreferences.TOKEN, null);
            String registration_id = sharedPreferences.getString(QRSyncPreferences.REGISTRATION_ID, null);

            if (token == null) {
                Toast.makeText(this, R.string.device_not_paired, Toast.LENGTH_SHORT).show();
                Intent startIntent = new Intent(this, MainActivity.class);
                startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startIntent);
                finish();
                return;
            }

            JSONObject json = new JSONObject();
            json.put("token", token);
            json.put("registration_id", registration_id);
            json.put("data", sharedText);

            String url = MySingleton.getURL() + "/send";
            JsonObjectRequest jsObjRequest = new JsonObjectRequest
                    (Request.Method.POST, url, json, new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                String status = response.getString("status");
                                if (status.equals("success")) {
                                    Toast.makeText(getApplicationContext(), R.string.send_successful, Toast.LENGTH_SHORT).show();
                                } else {
                                    String error = response.getString("error");
                                    Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                                    sharedPreferences.edit().remove(QRSyncPreferences.TOKEN).apply();
                                    Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
                                    startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(startIntent);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });

            MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        finish();
    }
}
