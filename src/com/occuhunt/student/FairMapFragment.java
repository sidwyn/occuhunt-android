package com.occuhunt.student;

import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import static com.occuhunt.student.FairActivity.EXTRA_FAIR_ID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FairMapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fair_map, container, false);
        
        long fairId = getActivity().getIntent().getExtras().getLong(FairActivity.EXTRA_FAIR_ID);
        Cursor roomsCursor = new DbHelper(getActivity()).queryRooms(fairId);
        
        roomsCursor.moveToFirst();
        long roomId = roomsCursor.getLong(roomsCursor.getColumnIndex(DbContract.RoomsTable._ID));
        updateMap(roomId, rootView);
        
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
            getActivity(),
            android.R.layout.simple_spinner_item,
            roomsCursor,
            new String[] { DbContract.RoomsTable.COLUMN_NAME_ROOM_NAME },
            new int[] { android.R.id.text1 },
            0
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        Spinner roomsSpinner = (Spinner) rootView.findViewById(R.id.rooms_spinner);
        roomsSpinner.setAdapter(adapter);
        roomsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long roomId) {
                updateMap(roomId, rootView);
            }
            
            @Override
            public void onNothingSelected(AdapterView parent) { }
        });
        
        return rootView;
    }
    
    public void updateMap(long roomId) {
        updateMap(roomId, getView());
    }
    
    private void updateMap(final long roomId, View rootView) {
        GridLayout gridLayout = (GridLayout) rootView.findViewById(R.id.fair_gridlayout);
        gridLayout.removeAllViews();
        
        final long fairId = getActivity().getIntent().getExtras().getLong(FairActivity.EXTRA_FAIR_ID);
        int baseDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 75, getResources().getDisplayMetrics());
        
        DbHelper dbHelper = new DbHelper(getActivity());
        String jsonUrl = "http://occuhunt.com/static/faircoords/" + fairId + "_" + roomId + ".json";
        JSONObject mapData = dbHelper.getJson(jsonUrl);
        
        View.OnClickListener listener = new View.OnClickListener() {
            public void onClick(View companyTile) {
                long companyId = Long.valueOf((String) companyTile.getTag());
                FragmentManager fm = getFragmentManager();
                CompanyFragment dialog = CompanyFragment.newInstance(companyId, fairId);
                dialog.show(fm, CompanyFragment.DIALOG_COMPANY);
            }
        };
        
        try {
            gridLayout.setColumnCount(mapData.getInt("cols"));
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
                    companyTile.setOnClickListener(listener);
                    
                    companyTile.setLayoutParams(params);
                    gridLayout.addView(companyTile);
                }
                else if (cell.getInt("blank_column") == 1 || cell.getInt("blank_row") == 1) {
                    TextView blankCell = new TextView(getActivity());
                    
                    if (cell.getInt("blank_column") == 1) blankCell.setWidth(baseDp * 3/5);
                    if (cell.getInt("blank_row") == 1)    blankCell.setHeight(baseDp * 3/5);
                    
                    blankCell.setLayoutParams(params);
                    gridLayout.addView(blankCell);
                }
            }
        } catch (JSONException e) {
            Log.e("FairMapFragment", e.toString());
        }
    }

}
