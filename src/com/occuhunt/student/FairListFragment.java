package com.occuhunt.student;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ListView;
import android.support.v4.widget.SimpleCursorAdapter;
import com.occuhunt.student.DbContract.CompaniesTable;

public class FairListFragment extends ListFragment {
    
    public static final String  EXTRA_FAIR_ID = "com.occuhunt.student.fair_id";
    private long mFairId;
    private FragmentActivity mContext;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mContext = (FragmentActivity) getActivity();
        final DbHelper dbHelper = new DbHelper(mContext);
        mFairId = mContext.getIntent().getExtras().getLong(EXTRA_FAIR_ID);
        
        Cursor companiesCursor = dbHelper.queryCompanies(mFairId);
        if (companiesCursor.getCount() == 0) { // Company data not downloaded for this fair yet
            dbHelper.fetchCompanies(mFairId, new Runnable() {
                @Override
                public void run() {
                    showCompaniesList(dbHelper.queryCompanies(mFairId));
                }
            });
        }
        else {
            showCompaniesList(companiesCursor);
        }
    }
    
    private void showCompaniesList(Cursor companiesCursor) {
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
            mContext,
            R.layout.single_textview,
            companiesCursor,
            new String[] { CompaniesTable.COLUMN_NAME_COMPANY_NAME },
            new int[] { R.id.single_textview },
            0
        );
        setListAdapter(adapter);
    }
    
    @Override
    public void onListItemClick(ListView listview, View view, int position, long companyId) {
        FragmentManager fm = mContext.getSupportFragmentManager();
        CompanyFragment dialog = CompanyFragment.newInstance(companyId, mFairId);
        dialog.show(fm, CompanyFragment.DIALOG_COMPANY);
    }

}
