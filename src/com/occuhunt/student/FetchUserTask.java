package com.occuhunt.student;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;

public class FetchUserTask extends FetchJSONTask {
    
    private JSONObject mUser;
    
    public FetchUserTask(Context context) {
        super(context);
    }
    
    @Override
    protected String doInBackground(String... linkedinId) {
        String[] url = new String[1];
        url[0] = Constants.API_URL + "/users/?linkedin_uid=" + linkedinId[0];
        return super.doInBackground(url);
    }
    
    @Override
    protected void onPostExecute(String jsonString) {
        super.onPostExecute(jsonString);
        try {
            mUser = getJSON().getJSONObject("response").getJSONArray("users").getJSONObject(0);
        } catch (Exception e) {
            Log.e("getUser()", e.toString());
        }
    }
    
    public JSONObject getUser() {
        return mUser;
    }
    
}
