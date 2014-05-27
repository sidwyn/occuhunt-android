package com.occuhunt.student;

import android.support.v4.app.Fragment;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import static com.occuhunt.student.DbHelper.PREF_LINKEDIN_ID;
import static com.occuhunt.student.DbHelper.PREF_RESUME_PATH;
import org.json.JSONException;
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
                    protected void onPostExecute(Void v) {
                        super.onPostExecute(v);
                        try {
                            String resumeUrl = getUser().getString("resume");
                            new DownloadFileTask(getActivity()) {
                                @Override
                                protected void onPreExecute() {
                                    mRootView.findViewById(R.id.linkedin_button).setVisibility(View.GONE);
                                    mRootView.findViewById(R.id.resume_frame).setVisibility(View.VISIBLE);
                                }
                                
                                @Override
                                protected void onPostExecute(String resumePath) {
                                    sharedPref.edit().putString(PREF_RESUME_PATH, resumePath).commit();
                                    showResume(resumePath);
                                }
                            }.execute(resumeUrl);
                        } catch (JSONException e) {
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
    
    private void showResume(final String resumePath) {
        final ImageView iv = (ImageView) mRootView.findViewById(R.id.resume_imageview);
        final View progressBar = mRootView.findViewById(R.id.resume_progressbar);
        
        Animation fadeOutProgressBar = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_out);
        fadeOutProgressBar.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation a) {
                progressBar.setVisibility(View.GONE);
            }
            
            @Override
            public void onAnimationStart(Animation a) {
            }
            
            @Override
            public void onAnimationRepeat(Animation a) {
            }
        });
        
        Animation fadeInResume = AnimationUtils.loadAnimation(getActivity(), R.anim.abc_fade_in);
        fadeInResume.setAnimationListener(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation a) {
                Drawable d = Drawable.createFromPath(resumePath);
                iv.setImageDrawable(d);
                iv.setVisibility(View.VISIBLE);
            }
            
            @Override
            public void onAnimationEnd(Animation a) {
                mAttacher = new PhotoViewAttacher(iv);
            }
            
            @Override
            public void onAnimationRepeat(Animation a) {
            }
        });
        
        progressBar.startAnimation(fadeOutProgressBar);
        iv.startAnimation(fadeInResume);
    }
 
}
