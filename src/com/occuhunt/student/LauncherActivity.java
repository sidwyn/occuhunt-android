package com.occuhunt.student;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;

public class LauncherActivity extends ActionBarActivity {

    private final DbHelper mDbHelper = new DbHelper(this);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.launcher_activity);

        if (! mDbHelper.isNetAvailable()) {
            findViewById(R.id.launcher_progress_bar).setVisibility(View.GONE);
            findViewById(R.id.launcher_error).setVisibility(View.VISIBLE);
        }
        else {
            mDbHelper.getReadableDatabase(); // Initialize db
            updateFairs();
        }
        
    }
    
    protected void updateFairs() {
        TextView launchStatus = (TextView) findViewById(R.id.launcher_status);
        launchStatus.setText("Updating fairs...");
        
        new FetchJSONTask(this, false) {
            @Override
            protected Void doInBackground(String... url) {
                super.doInBackground(url);
                try {
                    JSONArray fairsData = getJSON().getJSONArray("objects");
                    mDbHelper.insertFairs(fairsData);
                } catch (JSONException e) {
                    Log.e("updateFairs()", e.toString());
                }
                return null;
            }
            
            @Override
            protected void onPostExecute(Void v) {
                Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }.execute(Constants.API_URL + "/fairs/");
    }

}
