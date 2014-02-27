package com.occuhunt.student;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.os.Bundle;

public abstract class TabActivity extends Activity {

    protected abstract TabData[] tabDataArray();
    
    private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";
    protected ProgressDialog mDialog;
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(false);
        
        // Set up the action bar to show tabs.
        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // for each of the sections in the app, add a tab to the action bar.
        for (TabData tabData : tabDataArray()) {
            ActionBar.Tab tab = actionBar.newTab()
                           .setText(tabData.mTabTitle)
                           .setTabListener(new TabListener(this, getString(tabData.mTabTitle), tabData.mFragmentClass));
            actionBar.addTab(tab);
        }
        
        // Hide the Occuhunt icon
        // getActionBar().setDisplayShowHomeEnabled(false);
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {
      // Serialize the current tab position.
      outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
                      getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
      // Restore the previously serialized current tab position.
      if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
        getActionBar().setSelectedNavigationItem(savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
      }
    }

    public static class TabData<T extends Fragment> {
        int mTabTitle;
        Class<T> mFragmentClass;
        
        public TabData(int tabTitle, Class<T> fragmentClass) {
            mTabTitle = tabTitle;
            mFragmentClass = fragmentClass;
        }
    }
    
    public class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /** Constructor used each time a new tab is created.
          * @param activity  The host Activity, used to instantiate the fragment
          * @param tag  The identifier tag for the fragment
          * @param fragmentClass  The fragment's Class, used to instantiate the fragment
          */
        public TabListener(Activity activity, String tag, Class<T> fragmentClass) {
            mActivity = activity;
            mTag = tag;
            mClass = fragmentClass;
        }

        /* The following are each of the ActionBar.TabListener callbacks */

        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            // Check if the fragment is already initialized
            if (mFragment == null) {
                // If not, instantiate and add it to the activity
                mFragment = Fragment.instantiate(mActivity, mClass.getName());
                ft.add(android.R.id.content, mFragment, mTag);
            } else {
                // If it exists, simply attach it in order to show it
                ft.attach(mFragment);
            }
        }

        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            if (mFragment != null) {
                // Detach the fragment, because another one is being attached
                ft.detach(mFragment);
            }
        }

        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
            // User selected the already selected tab. Usually do nothing.
        }
    }
    
}
