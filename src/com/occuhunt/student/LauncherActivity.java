package com.occuhunt.student;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import org.json.JSONArray;

public class LauncherActivity extends Activity {

    private final DbHelper mDbHelper = new DbHelper(this);
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().hide();
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
        new FetchJSONTask(this) {
            @Override
            protected void onPostExecute(String jsonString) {
                super.onPostExecute(jsonString);
                try {
                    JSONArray fairsData = getJSON().getJSONArray("objects");
                    mDbHelper.insertFairs(fairsData);
                    
                    Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("updateFairs()", e.toString());
                }
            }
        }.execute(Constants.API_URL + "/fairs/");
    }

}
