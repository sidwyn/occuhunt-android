package com.occuhunt.student;

import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import org.json.JSONObject;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PortfolioFragment extends Fragment {

    PhotoViewAttacher mAttacher;
    public static final String PREF_LINKEDIN_ID = "LINKEDIN_ID";
    public static final String PREF_RESUME_PATH = "RESUME_PATH";
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.portfolio_fragment, container, false);
        updateResume(view);
        getActivity().setTitle(R.string.app_name);
        return view;
    }
    
    public void updateResume() {
        updateResume(getView());
    }
    
    private void updateResume(View rootView) {
        String linkedinId = getLinkedinId();
        
        if (linkedinId == null) {
            rootView.findViewById(R.id.linkedin_button).setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.resume_imageview).setVisibility(View.GONE);
        }
        else {
            rootView.findViewById(R.id.linkedin_button).setVisibility(View.GONE);
            rootView.findViewById(R.id.resume_imageview).setVisibility(View.VISIBLE);
            
            Drawable d = Drawable.createFromPath(getResumePath());
            ImageView imageView = (ImageView) rootView.findViewById(R.id.resume_imageview);
            imageView.setImageDrawable(d);
            mAttacher = new PhotoViewAttacher(imageView);
        }
    }
    
    private String getLinkedinId() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return sharedPref.getString(PREF_LINKEDIN_ID, null);
    }
    
    private String getResumePath() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String resumePath = sharedPref.getString(PREF_RESUME_PATH, null);
        
        if (resumePath == null) {
            String linkedinId = getLinkedinId();
            DbHelper dbHelper = new DbHelper(getActivity());
            JSONObject userData = dbHelper.getJson("http://occuhunt.com/api/v1/users/?linkedin_uid=" + linkedinId);
            
            try {
                JSONObject userArray = userData.getJSONObject("response").getJSONArray("users").getJSONObject(0);
                String resumeUrl = userArray.getString("resume");
                resumePath = dbHelper.new DownloadImageTask().execute(new String[] {resumeUrl}).get();
            } catch (Exception e) {
                return null;
            }
            sharedPref.edit().putString(PREF_RESUME_PATH, resumePath).commit();
        }
        
        return resumePath;
    }
    
}
