package com.occuhunt.student;

import android.provider.BaseColumns;
import static android.provider.BaseColumns._ID;

public final class DbContract {

    public static final  int    DATABASE_VERSION   = 1;
    public static final  String DATABASE_NAME      = "database.db";
    private static final String TEXT_TYPE          = " TEXT";
    private static final String INT_TYPE           = " INTEGER";
    private static final String COMMA_SEP          = ", ";
    
    public static final  String DATE_FORMAT        = "yyyy-MM-dd'T'HH:mm:ss";
    public static final  String REMOTE_ID_COLUMN   = "id";

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DbContract() {}

    public static abstract class FairsTable implements BaseColumns {
        public static final String TABLE_NAME = "fairs";
        public static final String COLUMN_NAME_FAIR_NAME = "name";
        public static final String COLUMN_NAME_LOGO = "logo";
        public static final String COLUMN_NAME_VENUE = "venue";
        public static final String COLUMN_NAME_TIME_START = "time_start";
        public static final String COLUMN_NAME_TIME_END = "time_end";

        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_FAIR_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_LOGO + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_VENUE + TEXT_TYPE + COMMA_SEP + 
                COLUMN_NAME_TIME_START + TEXT_TYPE + COMMA_SEP + 
                COLUMN_NAME_TIME_END + TEXT_TYPE + " )";
        
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    
    public static abstract class RoomsTable implements BaseColumns {
        public static final String TABLE_NAME = "rooms";
        public static final String COLUMN_NAME_ROOM_ID = "room_id";
        public static final String COLUMN_NAME_FAIR_ID = "fair_id";
        public static final String COLUMN_NAME_ROOM_NAME = "name";
        
        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_ROOM_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_FAIR_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_ROOM_NAME + TEXT_TYPE + COMMA_SEP + 
                " UNIQUE (" + COLUMN_NAME_ROOM_ID + ", " + COLUMN_NAME_FAIR_ID + " ))";
        
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    
    public static abstract class CompaniesTable implements BaseColumns {
        public static final String TABLE_NAME = "companies";
        public static final String COLUMN_NAME_COMPANY_NAME = "name";
        public static final String COLUMN_NAME_DESCRIPTION = "company_description";
        public static final String COLUMN_NAME_LOGO = "logo";
        
        public static final String REMOTE_ID_COLUMN = "coy_id";
        
        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_COMPANY_NAME + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                COLUMN_NAME_LOGO + TEXT_TYPE + " )";
        
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    
    public static abstract class FairsCompaniesTable implements BaseColumns {
        public static final String TABLE_NAME = "fairs_companies";
        public static final String COLUMN_NAME_FAIR_ID = "fair_id";
        public static final String COLUMN_NAME_ROOM_ID = "room_id";
        public static final String COLUMN_NAME_COMPANY_ID = "company_id";
        
        public static final String CREATE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                _ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_FAIR_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_ROOM_ID + INT_TYPE + COMMA_SEP +
                COLUMN_NAME_COMPANY_ID + INT_TYPE + COMMA_SEP +
                " UNIQUE (" + COLUMN_NAME_FAIR_ID + ", " + COLUMN_NAME_COMPANY_ID + "))";
                // + ", FOREIGN KEY (" + COLUMN_NAME_COMPANY_ID + ") REFERENCES " + CompaniesTable.TABLE_NAME + " (" + _ID + "))";
        
        public static final String SELECT_AND_JOIN_COMPANIES =
                "SELECT * FROM " + TABLE_NAME +
                " INNER JOIN " + CompaniesTable.TABLE_NAME +
                " ON " + COLUMN_NAME_COMPANY_ID + " = " + CompaniesTable.TABLE_NAME + "." + _ID;
        
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    
}
