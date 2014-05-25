package com.occuhunt.student;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.widget.ListView;
import android.support.v4.widget.SimpleCursorAdapter;

public class FairAdapter extends SimpleCursorAdapter
{
    public FairAdapter(Context context, Cursor c, String[] from, int[] to) {
        super(context, R.layout.fair_entry, c, from, to, 0);
    }
    
    public void onListItemClick(ListView listview, View view, int position, long id) {
        
    }
}
