package com.occuhunt.student;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import static com.occuhunt.student.DbHelper.PREF_LINKEDIN_ID;
import static com.occuhunt.student.DbHelper.PREF_RESUME_PATH;
import uk.co.senab.photoview.PhotoViewAttacher;

public class PortfolioFragment extends Fragment {

    PhotoViewAttacher mAttacher;
    View mRootView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.portfolio_fragment, container, false);
        updateResume();
        getActivity().setTitle(R.string.app_name);
        return mRootView;
    }
    
    public void updateResume() {
        final SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String linkedinId = sharedPref.getString(PREF_LINKEDIN_ID, null);
        
        if (linkedinId != null) { // User is logged in
            String resumePath = sharedPref.getString(PREF_RESUME_PATH, null);
            if (resumePath == null) {
                
                new FetchUserTask(getActivity()) {
                    @Override
                    protected void onPostExecute(String jsonString) {
                        super.onPostExecute(jsonString);
                        try {
                            String resumeUrl = getUser().getString("resume");
                            String resumePath = new DownloadFileTask(getActivity()).execute(new String[] {resumeUrl}).get();
                            sharedPref.edit().putString(PREF_RESUME_PATH, resumePath).commit();
                            showResume(resumePath);
                        } catch (Exception e) {
                            Log.e("getResumePath()", e.toString());
                        }
                    }
                }.execute(linkedinId);
                
            }
            else {
                showResume(resumePath);
            }
        }
        
    }
    
    private void showResume(String resumePath) {
        mRootView.findViewById(R.id.linkedin_button).setVisibility(View.GONE);
        mRootView.findViewById(R.id.resume_imageview).setVisibility(View.VISIBLE);

        Drawable d = Drawable.createFromPath(resumePath);
        ImageView imageView = (ImageView) mRootView.findViewById(R.id.resume_imageview);
        imageView.setImageDrawable(d);
        mAttacher = new PhotoViewAttacher(imageView);
    }
 
}
