package com.occuhunt.student;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class DropResumeActivity extends ListActivity {

    private List<Long> checkedIds = new ArrayList<Long>();
    private long mFairId;
    
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        DbHelper dbHelper = new DbHelper(this);
        
        Cursor fairCursor = dbHelper.queryNearestFair();
        fairCursor.moveToFirst();
        mFairId = fairCursor.getLong(fairCursor.getColumnIndex(DbContract.FairsTable._ID));
        
        String fairName = fairCursor.getString(fairCursor.getColumnIndex(DbContract.FairsTable.COLUMN_NAME_FAIR_NAME));
        setTitle(fairName);
        
        Cursor companiesCursor = dbHelper.queryCompanies(mFairId);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.company_entry,
                companiesCursor,
                new String[] { DbContract.CompaniesTable.COLUMN_NAME_COMPANY_NAME },
                new int[] { R.id.company_textview },
                0) {
                    
            @Override
            public View getView(int position, View oldView, ViewGroup parent) {
                View newView = super.getView(position, oldView, parent);
                
                CheckBox cb = (CheckBox) newView.findViewById(R.id.company_checkbox);
                boolean isChecked = checkedIds.contains(getItemId(position));
                cb.setChecked(isChecked);
                
                return newView;
            }
            
        };
        setListAdapter(adapter);
    }
    
    @Override
    public void onListItemClick(ListView listview, View view, int position, long companyId) {
        CheckBox checkbox = (CheckBox) view.findViewById(R.id.company_checkbox);
        checkbox.toggle();
        
        if (checkbox.isChecked()) {
            checkedIds.add(companyId);
        }
        else {
            checkedIds.remove(companyId);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drop_resume_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.action_drop_resume:
                dropResume();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    private void dropResume() {
        CompanyFragment cf = new CompanyFragment();
        for (long companyId : checkedIds) {
            if (cf.dropResume(this, companyId, mFairId) != 201) {
                Toast.makeText(this, "Error submitting resume.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        // Copied and pasted from showSuccessDialog() to fix context problems
        // 1. Instantiate an AlertDialog.Builder with its constructor
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // 2. Chain together various setter methods to set the dialog characteristics
        builder.setTitle(R.string.resume_drop_success)
               .setMessage(R.string.resume_drop_subtitle)
               .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) { }
                });

        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
