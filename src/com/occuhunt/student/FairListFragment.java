package com.occuhunt.student;

import android.app.FragmentManager;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.occuhunt.student.DbContract.CompaniesTable;

public class FairListFragment extends ListFragment {
    
    public static final String  EXTRA_FAIR_ID = "com.occuhunt.student.fair_id";
    private long mFairId;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        DbHelper dbHelper = new DbHelper(getActivity());
        mFairId = getActivity().getIntent().getExtras().getLong(EXTRA_FAIR_ID);
        
        Cursor cursor = dbHelper.queryCompanies(mFairId);
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
            getActivity(),
            R.layout.single_textview,
            cursor,
            new String[] { CompaniesTable.COLUMN_NAME_COMPANY_NAME },
            new int[] { R.id.single_textview },
            0
        );
        setListAdapter(adapter);
    }
    
    @Override
    public void onListItemClick(ListView listview, View view, int position, long companyId) {
        FragmentManager fm = getFragmentManager();
        CompanyFragment dialog = CompanyFragment.newInstance(companyId, mFairId);
        dialog.show(fm, CompanyFragment.DIALOG_COMPANY);
    }

}
