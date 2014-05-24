package com.occuhunt.student;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import com.occuhunt.student.DbContract.CompaniesTable;

public class FairListFragment extends ListFragment {
    
    public static final String  EXTRA_FAIR_ID = "com.occuhunt.student.fair_id";
    private long mFairId;
    private Activity mContext;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mContext = getActivity();
        final DbHelper dbHelper = new DbHelper(mContext);
        mFairId = mContext.getIntent().getExtras().getLong(EXTRA_FAIR_ID);
        
        Cursor companiesCursor = dbHelper.queryCompanies(mFairId);
        if (companiesCursor.getCount() == 0) { // Company data not downloaded for this fair yet
            Cursor roomsCursor = dbHelper.queryRooms(mFairId);
            int roomIdColumn = roomsCursor.getColumnIndex(DbContract.RoomsTable._ID);
            
            // TODO: Optimize this loop!
            while (roomsCursor.moveToNext()) {
                final long roomId = roomsCursor.getLong(roomIdColumn);
                final boolean isLastRoom = roomsCursor.isLast();
                
                new FetchJSONTask(mContext) {
                    @Override
                    protected void onPostExecute(String jsonString) {
                        super.onPostExecute(jsonString);
                        try {
                            dbHelper.insertCompaniesAtFair(getJSON().getJSONArray("coys"), mFairId, roomId);
                        } catch (Exception e) {
                            Log.e("queryCompanies()", e.toString());
                            return;
                        }
                        
                        // Company data should have been inserted, let's try again
                        if (isLastRoom) {
                            showCompaniesList(dbHelper.queryCompanies(mFairId));
                        }
                    }
                }.execute("http://occuhunt.com/static/faircoords/" + mFairId + "_" + roomId + ".json");
                
            }
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
        FragmentManager fm = getFragmentManager();
        CompanyFragment dialog = CompanyFragment.newInstance(companyId, mFairId);
        dialog.show(fm, CompanyFragment.DIALOG_COMPANY);
    }

}
