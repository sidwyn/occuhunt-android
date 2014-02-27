package com.occuhunt.student;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import static android.provider.BaseColumns._ID;
import android.util.Log;
import android.widget.ImageView;
import com.occuhunt.student.DbContract.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DbHelper extends SQLiteOpenHelper
{    
    private final Context mContext;
    private ProgressDialog mDialog;
    
    public DbHelper(Context context) {
        super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
        this.mContext = context;
        
        mDialog = new ProgressDialog(context);
        mDialog.setMessage("Loading...");
        mDialog.setCancelable(false);
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase db) {
        mDialog.show();
        db.execSQL(FairsTable.CREATE_TABLE);
        db.execSQL(RoomsTable.CREATE_TABLE);
        db.execSQL(CompaniesTable.CREATE_TABLE);
        db.execSQL(FairsCompaniesTable.CREATE_TABLE);
        
        try {
            JSONArray fairsData = getJson("http://occuhunt.com/api/v1/fairs/").getJSONArray("objects");
            insertFairs(fairsData, db);
            
            JSONArray companiesData = getJson("http://occuhunt.com/api/v1/companies/").getJSONObject("response").getJSONArray("companies");
            insertCompanies(companiesData, db);
        } catch (JSONException e) {
        } finally {
            mDialog.dismiss();
        }
    }

    // Method is called during an upgrade of the database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(FairsTable.DELETE_TABLE);
        db.execSQL(RoomsTable.DELETE_TABLE);
        db.execSQL(CompaniesTable.DELETE_TABLE);
        db.execSQL(FairsCompaniesTable.DELETE_TABLE);
        onCreate(db);
    }
    
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    
    public Cursor queryFairs() {
        String[] projection = {
            _ID, // Cursor must include _id column.
            FairsTable.COLUMN_NAME_FAIR_NAME,
            FairsTable.COLUMN_NAME_VENUE,
            FairsTable.COLUMN_NAME_LOGO,
            FairsTable.COLUMN_NAME_TIME_START,
            FairsTable.COLUMN_NAME_TIME_END
        };
        Cursor cursor = getReadableDatabase().query(
            FairsTable.TABLE_NAME,
            projection,
            null,
            null,
            null,
            null,
            FairsTable.COLUMN_NAME_TIME_END + " DESC"
        );
        return cursor;
    }
    
    public Cursor queryFair(long fairId) {
        Cursor c = getReadableDatabase().query(
            FairsTable.TABLE_NAME,
            null,
            _ID + " = ?",
            new String[] { String.valueOf(fairId) },
            null,
            null,
            null,
            "1"
        );
        return c;
        // return new FairsCursor(c).createFair();
    }
    
    public Cursor queryNearestFair() {
        return getReadableDatabase().query(
            FairsTable.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "ABS( strftime('%s', 'now') - strftime('%s', " + FairsTable.COLUMN_NAME_TIME_START + " ) )",
            "1"
        );
    }
    
    public Cursor queryCompany(long companyId) {
        return getReadableDatabase().query(
            CompaniesTable.TABLE_NAME,
            null,
            _ID + " = ?",
            new String[] { String.valueOf(companyId) },
            null,
            null,
            null,
            "1"
        );
    }
    
    public Cursor queryCompanies() {
        return getReadableDatabase().query(
            CompaniesTable.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            CompaniesTable.COLUMN_NAME_COMPANY_NAME
        );
    }
    
    public Cursor queryCompanies(long fairId) {
        String query = FairsCompaniesTable.SELECT_AND_JOIN_COMPANIES +
                       " WHERE " + FairsCompaniesTable.COLUMN_NAME_FAIR_ID + " = ?" +
                       " ORDER BY " + CompaniesTable.COLUMN_NAME_COMPANY_NAME;
        Cursor companiesCursor = getReadableDatabase().rawQuery(query,
                new String[] { String.valueOf(fairId) } );
        if (companiesCursor.moveToFirst()) {
            return companiesCursor;
        }
        else { // No company data available for this fair yet!
            mDialog.show();
            Cursor roomsCursor = queryRooms(fairId);
            int roomIdColumn = roomsCursor.getColumnIndex(RoomsTable._ID);
            
            // TODO: Optimize this loop!
            while (roomsCursor.moveToNext()) {
                try {
                    long roomId = roomsCursor.getLong(roomIdColumn);
                    String jsonUrl = "http://occuhunt.com/static/faircoords/" +
                            fairId + "_" + roomId + ".json";
                    insertCompaniesAtFair(getJson(jsonUrl).getJSONArray("coys"), fairId, roomId, getWritableDatabase());
                } catch (JSONException e) {
                    return null;
                }
            }
            mDialog.dismiss();
            return getReadableDatabase().rawQuery(query, new String[] { String.valueOf(fairId) } );
        }
    }
    
    public Cursor queryRooms(long fairId) {
        return getReadableDatabase().rawQuery(
            "SELECT " + RoomsTable.COLUMN_NAME_ROOM_ID + " AS " + _ID + ", " + RoomsTable.COLUMN_NAME_ROOM_NAME +
            " FROM " + RoomsTable.TABLE_NAME +
            " WHERE " + RoomsTable.COLUMN_NAME_FAIR_ID + " = ?",
            new String[] { String.valueOf(fairId) }
        );
    }
    
    /* Currently unused! */
    public class FairsCursor extends CursorWrapper
    {
        public FairsCursor(Cursor c) {
            super(c);
        }
        
        // Create a Fair object, constructed from the current row of the cursor
        public Fair createFair() {
            if (isBeforeFirst() || isAfterLast())
                return null;
            long fairId = getLong(getColumnIndex(_ID));
            String fairName = getString(getColumnIndex(FairsTable.COLUMN_NAME_FAIR_NAME)),
                   logoFilePath = getString(getColumnIndex(FairsTable.COLUMN_NAME_VENUE)),
                   venue = getString(getColumnIndex(FairsTable.COLUMN_NAME_LOGO)),
                   rawStartTime = getString(getColumnIndex(FairsTable.COLUMN_NAME_TIME_START)),
                   rawEndTime = getString(getColumnIndex(FairsTable.COLUMN_NAME_TIME_END));
            try {
                Calendar startTime = Calendar.getInstance();
                startTime.setTime(new SimpleDateFormat(DbContract.DATE_FORMAT).parse(rawStartTime));
                Calendar endTime = Calendar.getInstance();
                endTime.setTime(new SimpleDateFormat(DbContract.DATE_FORMAT).parse(rawEndTime));
                
                return new Fair(fairId, fairName, logoFilePath, venue, startTime, endTime);
            } catch (Exception e) {
                Log.e("getFair()", e.toString());
            }
            return null;
        }
    }
    
    private void insertFairs(JSONArray fairsData, SQLiteDatabase db) {
        for (int i=0; i < fairsData.length(); i++) {
            try {
                JSONObject fairData = fairsData.getJSONObject(i);
                
                // TODO: Optimize DownloadImageTask() to download all images at one go
                String logoPath = new DownloadImageTask().execute(fairData.getString("logo")).get();
                
                ContentValues fairEntry = new ContentValues();
                fairEntry.put(_ID, fairData.getString(DbContract.REMOTE_ID_COLUMN));
                fairEntry.put(FairsTable.COLUMN_NAME_FAIR_NAME, fairData.getString(FairsTable.COLUMN_NAME_FAIR_NAME));
                fairEntry.put(FairsTable.COLUMN_NAME_LOGO, logoPath);
                fairEntry.put(FairsTable.COLUMN_NAME_VENUE, fairData.getString(FairsTable.COLUMN_NAME_VENUE));
                fairEntry.put(FairsTable.COLUMN_NAME_TIME_START, fairData.getString(FairsTable.COLUMN_NAME_TIME_START));
                fairEntry.put(FairsTable.COLUMN_NAME_TIME_END, fairData.getString(FairsTable.COLUMN_NAME_TIME_END));
                long fairId = db.insert(FairsTable.TABLE_NAME, null, fairEntry);
                
                insertRooms(fairData.getJSONArray("rooms"), fairId, db);
            } catch (Exception e) {
                // Meh.
            }
        }
    }
    
    private void insertRooms(JSONArray roomsData, long fairId, SQLiteDatabase db) {
        for (int j=0; j < roomsData.length(); j++) {
            try {
                JSONObject roomData = roomsData.getJSONObject(j);

                ContentValues roomEntry = new ContentValues();
                long roomId = roomData.getLong(DbContract.REMOTE_ID_COLUMN);
                roomEntry.put(RoomsTable.COLUMN_NAME_ROOM_ID, roomId);
                roomEntry.put(RoomsTable.COLUMN_NAME_FAIR_ID, fairId);
                roomEntry.put(RoomsTable.COLUMN_NAME_ROOM_NAME, roomData.getString(RoomsTable.COLUMN_NAME_ROOM_NAME));
                db.insert(RoomsTable.TABLE_NAME, null, roomEntry);
            } catch (Exception e) {
                // Meh.
            }
        }
    }
    
    private void insertCompaniesAtFair(JSONArray companiesData, long fairId, long roomId, SQLiteDatabase db) {
        for (int i=0; i < companiesData.length(); i++) {
            try {
                JSONObject companyData = companiesData.getJSONObject(i);
                long companyId = companyData.getLong(CompaniesTable.REMOTE_ID_COLUMN);

                if (companyId != 0) {
                    ContentValues fairCompanyEntry = new ContentValues();
                    fairCompanyEntry.put(FairsCompaniesTable.COLUMN_NAME_FAIR_ID, fairId);
                    fairCompanyEntry.put(FairsCompaniesTable.COLUMN_NAME_ROOM_ID, roomId);
                    fairCompanyEntry.put(FairsCompaniesTable.COLUMN_NAME_COMPANY_ID, companyId);
                    db.insertWithOnConflict(FairsCompaniesTable.TABLE_NAME, null, fairCompanyEntry, SQLiteDatabase.CONFLICT_REPLACE);
                }
            } catch (Exception e) {
                // Meh.
            }
        }
    }
    
    private void insertCompanies(JSONArray companiesData, SQLiteDatabase db) {
        for (int i=0; i < companiesData.length(); i++) { // TODO: Change this to insert all companies, not just the first 30
            try {
                JSONObject companyData = companiesData.getJSONObject(i);
                
                ContentValues companyEntry = new ContentValues();
                companyEntry.put(_ID, companyData.getString(DbContract.REMOTE_ID_COLUMN));
                companyEntry.put(CompaniesTable.COLUMN_NAME_LOGO, companyData.getString(CompaniesTable.COLUMN_NAME_LOGO));
                companyEntry.put(CompaniesTable.COLUMN_NAME_COMPANY_NAME, companyData.getString(CompaniesTable.COLUMN_NAME_COMPANY_NAME));
                companyEntry.put(CompaniesTable.COLUMN_NAME_DESCRIPTION, companyData.getString(CompaniesTable.COLUMN_NAME_DESCRIPTION));
                db.insert(CompaniesTable.TABLE_NAME, null, companyEntry);
            } catch (Exception e) {
                Log.e("insertCompanies", e.toString());
            }
        }
    }
    
    private long getUserId(String linkedinId) {
        JSONObject userJson = getJson("http://occuhunt.com/api/v1/users/?linkedin_uid=" + linkedinId);
        try {
            return userJson.getJSONObject("response").getJSONArray("users").getJSONObject(0).getLong("id");
        } catch (Exception e) {
            Log.e("getUserId()", e.toString());
            return 0;
        }
    }
    
    public JSONObject getJson(String url) {
        try {
            String jsonString = new FetchJsonTask().execute(url).get();
            return new JSONObject(jsonString);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    private class FetchJsonTask extends AsyncTask<String, Void, String> {
        
        @Override
        protected void onPreExecute() {
            mDialog.show();
        }
        
        @Override
        protected String doInBackground(String... url) {
            HttpClient httpclient = new DefaultHttpClient(); // for port 80 requests!
            HttpGet httpget = new HttpGet(url[0]);
            InputStream is;
            
            try {
                HttpEntity httpentity = httpclient.execute(httpget).getEntity();
                is = httpentity.getContent();
            } catch (IOException e) {
                Log.e("FetchJson", e.toString());
                return null;
            }
            
            // Read response to string
            try {	    	
                BufferedReader reader = new BufferedReader(new InputStreamReader(is,"utf-8"), 8);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line + "\n");
                }
                is.close();
                return sb.toString();           
            } catch (Exception e) {
                Log.e("getJson()", e.toString());
                return null;
            }
        }
        
        @Override
        protected void onPostExecute(String jsonResult) {
            mDialog.dismiss();
        }
    }
    
    public class DownloadImageTask extends AsyncTask<String, Void, String> {
        
        @Override
        protected String doInBackground(String... url) {
            String fullPath;

            try {
                InputStream input = new URL(url[0]).openStream();
                String filename = url[0].substring(url[0].lastIndexOf('/') + 1);

                File filesDir = mContext.getFilesDir();
                fullPath = filesDir.toString() + "/" + filename;
                OutputStream output = new FileOutputStream(fullPath);

                try {
                    byte[] buffer = new byte[1024];
                    int bytesRead = 0;

                    while ((bytesRead = input.read(buffer, 0, buffer.length)) >= 0) {
                        output.write(buffer, 0, bytesRead);
                    }
                } finally {
                    output.close();
                    input.close();
                }
            } catch (Exception e) {
                Log.e("DownloadImageTask", e.toString());
                return null;
            }

            return fullPath;
        }
    }
}