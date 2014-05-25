package com.occuhunt.student;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

public class CompanyFragment extends DialogFragment {

    public static final String DIALOG_COMPANY = "company_details";
    public static final String EXTRA_COMPANY_ID = "com.occuhunt.student.company_id";
    public static final String EXTRA_FAIR_ID = "com.occuhunt.student.fair_id";
    private long mCompanyId, mFairId;
    
    public static CompanyFragment newInstance(long companyId, long fairId) {
        Bundle args = new Bundle();
        args.putLong(EXTRA_COMPANY_ID, companyId);
        args.putLong(EXTRA_FAIR_ID, fairId);
        
        CompanyFragment fragment = new CompanyFragment();
        fragment.setArguments(args);
        
        return fragment;
    }
    
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Instantiate data for this instance
        mCompanyId = getArguments().getLong(EXTRA_COMPANY_ID);
        mFairId = getArguments().getLong(EXTRA_FAIR_ID);
        DbHelper dbHelper = new DbHelper(getActivity());
        
        Cursor c = dbHelper.queryCompany(mCompanyId);
        c.moveToFirst();
        String logoUrl = c.getString(c.getColumnIndex(DbContract.CompaniesTable.COLUMN_NAME_LOGO));
        String description = c.getString(c.getColumnIndex(DbContract.CompaniesTable.COLUMN_NAME_DESCRIPTION));
        
        View view = getActivity().getLayoutInflater().inflate(R.layout.company_dialog, null);
        ((TextView) view.findViewById(R.id.company_description)).setText(description);
        
        ImageView logoImageView = (ImageView) view.findViewById(R.id.company_logo);
        new FetchImageTask(logoImageView).execute(logoUrl);
        
        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.title_drop_resume, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) {
                        int statusCode = dropResume(getActivity(), mCompanyId, mFairId);
                        if (statusCode == 201) showSuccessDialog();
                    }
                })
                .setNegativeButton(R.string.button_close, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int buttonId) { }
                })
                .create();
    }
    
    public void showSuccessDialog() {
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.resume_drop_success)
               .setMessage(R.string.resume_drop_subtitle)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    /**
     * Drop user's resume for the specified company and fair.
     * @param activity Because this fragment might not be added, getActivity() may return null
     * @param companyId
     * @param fairId
     * @return statusCode
     */
    public int dropResume(Activity activity, long companyId, long fairId) {
        Log.d("Custom debug", activity.toString());
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(activity);
        long userId = sharedPref.getLong(MainActivity.PREF_USER_ID, 0);
        Log.d("Custom debug", "User ID: " + String.valueOf(userId));
        if (userId == 0) {
            Toast.makeText(activity, "Please sign in to drop your resume.", Toast.LENGTH_SHORT).show();
            return 0;
        }
            
        try {
            return new DropResumeTask().execute(companyId, fairId, userId).get();
        } catch (Exception e) {
            Log.e("dropResume()", e.toString());
            return 0;
        }
    }
    
    private class DropResumeTask extends AsyncTask<Long, Void, Integer> {
        
        /**
         * 
         * @param ids Array containing companyId, fairId and userId.
         * @return statusCode
         */
        @Override
        protected Integer doInBackground(Long... ids) {
            // Create a new HttpClient and Post Header
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://occuhunt.com/api/v1/applications/");

            try {
                JSONObject input = new JSONObject();
                input.put("company_id", ids[0]);
                input.put("fair_id", ids[1]);
                input.put("user_id", ids[2]);
                input.put("status", 1);
                StringEntity params = new StringEntity(input.toString());
                httppost.setEntity(params);
                httppost.setHeader("Content-Type", "application/json");
                
                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);
                return response.getStatusLine().getStatusCode();
            } catch (Exception e) {
                Log.e("DropResumeTask", e.toString());
                return null;
            }
        }
    }
    
    private class FetchImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView mImageView;

        public FetchImageTask(ImageView imageView) {
            mImageView = imageView;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (IOException e) {
                Log.e("Error", e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            mImageView.setImageBitmap(result);
        }
    }

}
