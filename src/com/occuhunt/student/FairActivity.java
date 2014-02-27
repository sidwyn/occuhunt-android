package com.occuhunt.student;

import android.database.Cursor;
import android.os.Bundle;

public class FairActivity extends TabActivity {

    public static final String EXTRA_FAIR_ID = "com.occuhunt.student.fair_id";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); 
        
        DbHelper dbHelper = new DbHelper(this);
        long fairId = getIntent().getExtras().getLong(EXTRA_FAIR_ID);
        
        Cursor c = dbHelper.queryFair(fairId);
        c.moveToFirst();
        String fairName = c.getString(c.getColumnIndex(DbContract.FairsTable.COLUMN_NAME_FAIR_NAME));
        setTitle(fairName);
    }
    
    protected TabData[] tabDataArray() {
        return new TabData[] {
            new TabData(R.string.title_fair_map, FairMapFragment.class),
            new TabData(R.string.title_fair_list, FairListFragment.class)
        };
    }

}
