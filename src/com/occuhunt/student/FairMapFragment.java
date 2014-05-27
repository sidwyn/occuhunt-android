package com.occuhunt.student;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.support.v7.widget.GridLayout;
import android.support.v4.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FairMapFragment extends Fragment {

    View mRootView;
    GridLayout mGridLayout;
    long mFairId;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fair_map, container, false);
        mGridLayout = (GridLayout) mRootView.findViewById(R.id.fair_gridlayout);
        
        mFairId = getActivity().getIntent().getExtras().getLong(FairActivity.EXTRA_FAIR_ID);
        Cursor roomsCursor = new DbHelper(getActivity()).queryRooms(mFairId);
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
            getActivity(),
            android.R.layout.simple_spinner_item,
            roomsCursor,
            new String[] { DbContract.RoomsTable.COLUMN_NAME_ROOM_NAME },
            new int[] { android.R.id.text1 },
            0
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        Spinner roomsSpinner = (Spinner) mRootView.findViewById(R.id.rooms_spinner);
        roomsSpinner.setAdapter(adapter);
        roomsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long roomId) {
                updateMap(roomId);
            }
            
            @Override
            public void onNothingSelected(AdapterView parent) { }
        });
        
        return mRootView;
    }
    
    private void updateMap(final long roomId) {
        
        new FetchJSONTask(getActivity()) {
            @Override
            protected void onPostExecute(Void v) {
                super.onPostExecute(v);
                try {
                    updateLayout(getJSON());
                } catch (JSONNotFoundException e) {
                    TextView errorText = new TextView(getActivity());
                    errorText.setText("Sorry, map data is not available for this fair.");
                    mGridLayout.addView(errorText);
                    Log.e("updateMap()", e.toString());
                }
            }
        }.execute("http://occuhunt.com/static/faircoords/" + mFairId + "_" + roomId + ".json");
        
    }
    
    private void updateLayout(JSONObject mapData) {
        mGridLayout.removeAllViews();
        
        int baseDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, getResources().getDisplayMetrics());
        
        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View companyTile) {
                long companyId = Long.valueOf((String) companyTile.getTag());
                FragmentManager fm = getActivity().getSupportFragmentManager();
                CompanyFragment dialog = CompanyFragment.newInstance(companyId, mFairId);
                dialog.show(fm, CompanyFragment.DIALOG_COMPANY);
            }
        };
        
        try {
            mGridLayout.setColumnCount(mapData.getInt("cols"));
            JSONArray cellsArray = mapData.getJSONArray("coys");
            
            for (int i=0; i < cellsArray.length(); i++) {
                JSONObject cell = cellsArray.getJSONObject(i);
                
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.columnSpec = GridLayout.spec(cell.getInt("x") - 1, GridLayout.FILL);
                params.rowSpec = GridLayout.spec(cell.getInt("y") - 1, GridLayout.FILL);
                
                if (cell.getInt("coy_id") != 0) {
                    Button companyTile = new Button(getActivity());
                    companyTile.setText(cell.getString("coy_name"));
                    companyTile.setTag(cell.getString("coy_id"));
                    companyTile.setWidth(baseDp);
                    companyTile.setHeight(baseDp);
                    companyTile.setTextSize(11);
                    companyTile.setMaxLines(4);
                    companyTile.setOnClickListener(listener);
                    
                    companyTile.setLayoutParams(params);
                    mGridLayout.addView(companyTile);
                }
                else if (cell.getInt("blank_column") == 1 || cell.getInt("blank_row") == 1) {
                    TextView blankCell = new TextView(getActivity());
                    
                    if (cell.getInt("blank_column") == 1) blankCell.setWidth(baseDp * 3/5);
                    if (cell.getInt("blank_row") == 1)    blankCell.setHeight(baseDp * 3/5);
                    
                    blankCell.setLayoutParams(params);
                    mGridLayout.addView(blankCell);
                }
            }
        } catch (JSONException e) {
            Log.e("FairMapFragment", e.toString());
        }
    }

}
