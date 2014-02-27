package com.occuhunt.student;

import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import com.google.code.linkedinapi.client.AsyncLinkedInApiClient;
import com.google.code.linkedinapi.client.LinkedInApiClientFactory;
import com.google.code.linkedinapi.client.enumeration.ProfileField;
import com.google.code.linkedinapi.client.oauth.LinkedInAccessToken;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.client.oauth.LinkedInRequestToken;
import com.google.code.linkedinapi.schema.Person;
import java.util.EnumSet;
import java.util.concurrent.Future;
import org.json.JSONObject;

public class MainActivity extends TabActivity
{
    public static final String PREF_USER_ID = "USER_ID";
    
    private static final LinkedInOAuthService mOAuthService = 
            LinkedInOAuthServiceFactory.getInstance().createLinkedInOAuthService(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET);
    private static LinkedInRequestToken mLiToken;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new DbHelper(this).getReadableDatabase(); // Initialize DB
    }
    
    protected TabData[] tabDataArray() {
        return new TabData[] {
            new TabData(R.string.title_portfolio, PortfolioFragment.class),
            new TabData(R.string.title_fairs, FairsFragment.class)
        };
    }
    
    public void showCompanies(View view) {
        Intent intent = new Intent(this, DropResumeActivity.class);
        startActivity(intent);
    }
    
    public void linkedinLogin(View view) {
        new RequestTokenTask().execute();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        
        if (getIntent().getData() != null) { // User just returned to Activity from Linkedin login
            mDialog.show();
            new LinkedinAuthTask().execute();
        }
    }
    
    private class RequestTokenTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... args) {
            mLiToken = mOAuthService.getOAuthRequestToken(Constants.OAUTH_CALLBACK_URL);
            return null;
        }
        
        @Override
        protected void onPostExecute(Void arg) {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mLiToken.getAuthorizationUrl()));
            startActivity(i);
        }
    }
    
    private class LinkedinAuthTask extends AsyncTask<Void, Void, LinkedInAccessToken> {
        @Override
        protected LinkedInAccessToken doInBackground(Void... args) {
            String verifier = getIntent().getData().getQueryParameter("oauth_verifier");
            return mOAuthService.getOAuthAccessToken(mLiToken, verifier);
        }
        
        @Override
        protected void onPostExecute(LinkedInAccessToken accessToken) {
            final LinkedInApiClientFactory factory = LinkedInApiClientFactory.newInstance(Constants.CONSUMER_KEY, Constants.CONSUMER_SECRET);

            AsyncLinkedInApiClient client = factory.createAsyncLinkedInApiClient(accessToken);
            Future<Person> asyncResult = client.getProfileForCurrentUser(EnumSet.of(ProfileField.ID));
            String linkedinId = null;
            
            try {
                Person profile = asyncResult.get();
                linkedinId = profile.getId();
            } catch (Exception e) {
                Log.e("LinkedinAuthTask", e.toString());
            }
            
            long userId = getUserId(linkedinId);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            sharedPref.edit().clear()
                    .putString(PortfolioFragment.PREF_LINKEDIN_ID, linkedinId)
                    .putLong(PREF_USER_ID, userId)
                    .commit();
            
            // Re-instantiate the Portfolio fragment
            FragmentManager fm = getFragmentManager();
            PortfolioFragment fragment = (PortfolioFragment) fm.findFragmentByTag(getString(R.string.title_portfolio));
            fragment.updateResume();
            
            mDialog.dismiss();
        }
    }
    
    private long getUserId(String linkedinId) {
        JSONObject userJson = new DbHelper(this).getJson("http://occuhunt.com/api/v1/users/?linkedin_uid=" + linkedinId);
        try {
            return userJson.getJSONObject("response").getJSONArray("users").getJSONObject(0).getLong("id");
        } catch (Exception e) {
            Log.e("getUserId()", e.toString());
            return 0;
        }
    }
}
