package com.occuhunt.student;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

public class FetchJSONTask extends AsyncTask<String, Void, Void> {
    
    private String mJSONUrl;
    private String mJSONString;
    private JSONObject mJSONResult;
    protected final Context mContext;
    private final ProgressDialog mDialog;
    private boolean mShowDialog;
    
    public FetchJSONTask(Context context) {
        mContext = context;
        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(false);
        mShowDialog = true;
    }
    
    public FetchJSONTask(Context context, boolean showProgressDialog) {
        this(context);
        mShowDialog = showProgressDialog;
    }
    
    @Override
    protected void onPreExecute() {
        if ( ! new DbHelper(mContext).isNetAvailable()) {
            Toast.makeText(mContext, R.string.no_internet_error_msg, Toast.LENGTH_SHORT).show();
        }
        else if (mShowDialog) {
            mDialog.show();
        }
    }
    
    @Override
    protected Void doInBackground(String... url) {
        HttpClient httpclient = new DefaultHttpClient(); // for port 80 requests!
        HttpGet httpget = new HttpGet(url[0]);
        InputStream is;
        mJSONUrl = url[0];
        
        try {
            HttpResponse response = httpclient.execute(httpget);
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                return null;
            }
            HttpEntity httpentity = response.getEntity();
            is = httpentity.getContent();
        } catch (IOException e) {
            Log.e("FetchJSONTask", e.toString());
            return null;
        }

        // Read response to string
        try {	    	
            BufferedReader reader = new BufferedReader(new InputStreamReader(is,"utf-8"), 8);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            is.close();
            mJSONString = sb.toString();           
        } catch (IOException e) {
            Log.e("FetchJSONTask", e.toString());
            return null;
        }
        
        if (mJSONString != null) {
            // Toast.makeText(mContext, R.string.download_error_msg, Toast.LENGTH_SHORT).show();
            try {
                mJSONResult = new JSONObject(mJSONString);
            } catch (JSONException e) {
                Log.e("FetchJSONTask", e.toString() + " on URL: " + mJSONUrl);
            }
        }
        
        return null;
    }
        
    @Override
    protected void onPostExecute(Void v) {
        mDialog.dismiss();
    }
    
    protected JSONObject getJSON() throws JSONNotFoundException {
        if (mJSONResult == null) {
            throw new JSONNotFoundException("Attempted to get null JSON object.");
        }
        return mJSONResult;
    }
}