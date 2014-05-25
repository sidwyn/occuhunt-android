package com.occuhunt.student;

import android.support.v4.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.occuhunt.student.DbContract.FairsTable;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class FairsFragment extends ListFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   
        getActivity().setTitle(R.string.title_fairs);
        
        Cursor cursor = new DbHelper(getActivity()).queryFairs();
        
        String[] from = {FairsTable.COLUMN_NAME_FAIR_NAME, FairsTable.COLUMN_NAME_VENUE, FairsTable.COLUMN_NAME_LOGO, FairsTable.COLUMN_NAME_TIME_START};
        int[] to = {R.id.fair_name, R.id.venue, R.id.fair_logo, R.id.fair_duration};
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
            getActivity(),
            R.layout.fair_entry,
            cursor,
            from,
            to,
            0
        );
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                int timeStartColumn = cursor.getColumnIndex(FairsTable.COLUMN_NAME_TIME_START),
                    timeEndColumn   = cursor.getColumnIndex(FairsTable.COLUMN_NAME_TIME_END);
                
                if (columnIndex == timeStartColumn) {
                    String duration = formatDuration(cursor.getString(timeStartColumn), cursor.getString(timeEndColumn));
                    ((TextView) view).setText(duration);
                    return true;
                }
                return false; // Let SimpleCursorAdapter do its thang.
            }
        });
        setListAdapter(adapter);
    }
    
    private String formatDuration(String rawStartTime, String rawEndTime) {
        Calendar startTime = Calendar.getInstance(),
                 endTime = Calendar.getInstance();
        SimpleDateFormat fullTime = new SimpleDateFormat("MMM d, h:mm a"),
                 timeOnly = new SimpleDateFormat("h:mm a");
                 
        try {
            startTime.setTime(new SimpleDateFormat(DbContract.DATE_FORMAT).parse(rawStartTime));
            endTime.setTime(new SimpleDateFormat(DbContract.DATE_FORMAT).parse(rawEndTime));
        } catch (Exception e) {
            Log.w("formatDuration()", e.toString());
            return null;
        }
        
        if (startTime.get(Calendar.DAY_OF_MONTH) == endTime.get(Calendar.DAY_OF_MONTH)) {
            return fullTime.format(startTime.getTime()) + " - " + timeOnly.format(endTime.getTime());
        }
        else {
            return fullTime.format(startTime.getTime()) + " - " + fullTime.format(endTime.getTime());
        }
    }
    
    @Override
    public void onListItemClick(ListView listview, View view, int position, long fairId) {
        /* Cursor cursor = ((SimpleCursorAdapter) listview.getAdapter()).getCursor();
        cursor.moveToPosition(position); */
        Intent intent = new Intent(getActivity(), FairActivity.class);
        intent.putExtra(FairActivity.EXTRA_FAIR_ID, fairId);
        startActivity(intent);
    }
    
    /* private static class FairsCursorAdapter extends CursorAdapter {
        
        private FairsCursor mFairsCursor;
        
        public FairsCursorAdapter(Context context, FairsCursor cursor) {
            super(context, cursor, 0);
            mFairsCursor = cursor;
        }
        
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            return inflater.inflate(R.layout.fair_entry, parent, false);
        }
        
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            Fair fair = mFairsCursor.createFair();
            
        }
    } */
    
}
