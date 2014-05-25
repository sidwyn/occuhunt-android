package com.occuhunt.student;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import static android.provider.BaseColumns._ID;
import android.util.Log;
import com.occuhunt.student.DbContract.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONObject;

public class DbHelper extends SQLiteOpenHelper
{    
    private final Context mContext;
    
    public static final String PREF_LINKEDIN_ID = "LINKEDIN_ID";
    public static final String PREF_RESUME_PATH = "RESUME_PATH";
    public static final String PREF_FAIRS_UPDATED = "FAIRS_UPDATED";
    
    public DbHelper(Context context) {
        super(context, DbContract.DATABASE_NAME, null, DbContract.DATABASE_VERSION);
        this.mContext = context;
    }

    // Method is called during creation of the database
    @Override
    public void onCreate(final SQLiteDatabase db) {
        db.execSQL(FairsTable.CREATE_TABLE);
        db.execSQL(RoomsTable.CREATE_TABLE);
        db.execSQL(CompaniesTable.CREATE_TABLE);
        db.execSQL(FairsCompaniesTable.CREATE_TABLE);
        
        fetchCompanies();
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
    
    public Cursor queryNextFair() {
        String secondsTillEnd = "strftime('%s', " + FairsTable.COLUMN_NAME_TIME_END + " ) - strftime('%s', 'now')";
        
        return getReadableDatabase().query(
            FairsTable.TABLE_NAME,
            null,
            secondsTillEnd + " > -86400", // Valid till 1 day after event
            null,
            null,
            null,
            secondsTillEnd,
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
    
    public Cursor queryAllCompanies() {
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
    
    public Cursor queryCompanies(final long fairId) {
        String query = FairsCompaniesTable.SELECT_AND_JOIN_COMPANIES +
                       " WHERE " + FairsCompaniesTable.COLUMN_NAME_FAIR_ID + " = ?" +
                       " ORDER BY " + CompaniesTable.COLUMN_NAME_COMPANY_NAME;
        Cursor companiesCursor = getReadableDatabase().rawQuery(query,
                new String[] { String.valueOf(fairId) } );
        
        return companiesCursor;
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
    
    protected void insertFairs(JSONArray fairsData) {
        SQLiteDatabase db = getWritableDatabase();
        
        for (int i=0; i < fairsData.length(); i++) {
            try {
                JSONObject fairData = fairsData.getJSONObject(i);
                
                // TODO: Optimize DownloadFileTask() to download all images at one go
                String logoPath = new DownloadFileTask(mContext).execute(fairData.getString(FairsTable.COLUMN_NAME_LOGO)).get();
                long fairId = fairData.getLong(DbContract.REMOTE_ID_COLUMN);
                
                if (queryFair(fairId).getCount() == 0) {
                    ContentValues fairEntry = new ContentValues();
                    fairEntry.put(_ID, fairId);
                    fairEntry.put(FairsTable.COLUMN_NAME_FAIR_NAME, fairData.getString(FairsTable.COLUMN_NAME_FAIR_NAME));
                    fairEntry.put(FairsTable.COLUMN_NAME_LOGO, logoPath);
                    fairEntry.put(FairsTable.COLUMN_NAME_VENUE, fairData.getString(FairsTable.COLUMN_NAME_VENUE));
                    fairEntry.put(FairsTable.COLUMN_NAME_TIME_START, fairData.getString(FairsTable.COLUMN_NAME_TIME_START));
                    fairEntry.put(FairsTable.COLUMN_NAME_TIME_END, fairData.getString(FairsTable.COLUMN_NAME_TIME_END));
                    db.insert(FairsTable.TABLE_NAME, null, fairEntry);

                    insertRooms(fairData.getJSONArray("rooms"), fairId, db);
                }
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
    
    protected void insertCompaniesAtFair(JSONArray companiesData, long fairId, long roomId) {
        SQLiteDatabase db = getWritableDatabase();
        
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
                Log.e("insertCompaniesAtFair", e.toString());
            }
        }
    }
    
    public void fetchCompanies() {
        new FetchJSONTask(mContext) {
            @Override
            protected void onPostExecute(String jsonString) {
                try {
                    super.onPostExecute(jsonString);
                    JSONArray companiesData = getJSON().getJSONObject("response").getJSONArray("companies");
                    insertCompanies(companiesData);
                } catch (Exception e) {
                    Log.e("onCreate()", e.toString());
                }
            }
        }.execute(Constants.API_URL + "/companies/");
    }
    
    private void insertCompanies(JSONArray companiesData) {
        SQLiteDatabase db = getWritableDatabase();
        
        for (int i=0; i < companiesData.length(); i++) {
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
    
    // -------------------------------------------------------------------------
    
    public boolean isNetAvailable() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobileInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            if (wifiInfo.isConnected() || mobileInfo.isConnected()) {
                return true;
            }
        }
        catch (Exception e) {
           e.printStackTrace();
        }
        return false;
    }
}