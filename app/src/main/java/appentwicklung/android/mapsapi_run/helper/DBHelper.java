package appentwicklung.android.mapsapi_run.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


import java.util.ArrayList;
import java.util.List;

import appentwicklung.android.mapsapi_run.model.WorkoutLocation;
import appentwicklung.android.mapsapi_run.model.WorkoutSession;

/**
 * The class handles the SQLite database used in the app
 *
 * @author Daniel Johansson
 */
public class DBHelper extends SQLiteOpenHelper {

    private final boolean DBG = true;//Zum Debuggen
    private static final String TAG = "DBHelper"; //Zum Debuggen, gibt Classe an
    
    //______________ Database Variables______________
        //My Database Object
      private SQLiteDatabase myDataBase;
        //Our SQlite Database Name
     private static final String DATABASE_NAME = "runner.db";
        //Tabel 1:for every SessioN
           private static final String SESSIONS_TABLE = "sessions";
        //Colum names for Tabel1
         private static final String KEY_SESSIONS_ID = "id";
        private static final String KEY_STARTTIME = "session_start_zeit";
        private static final String KEY_STARTDATE = "session_start_datum";
        private static final String KEY_DURATION = "dauer";
        private static final String KEY_DISTANCE = "distanz";
        //Tabel 2: the Locations for eache session(sameId)
        private static final String LOCATIONS_TABLE = "mylocations";
        //Colum names for Tabel2
       private static final String KEY_LOCATIONS_ID = "id";
       private static final String KEY_LOCATIONS_SESSIONS_ID = "locationsession_id";
        private static final String KEY_LATITUDE = "latitude";
        private static final String KEY_LONGITUDE = "longitude";
        private static final String KEY_SPEED = "geschwindigkeit";
        private static final String KEY_ELAPSED_TIME = "vergangene_zeit";
        //Sessions table Create statement
        private static final String CREATE_SESSION_TABLE = "CREATE TABLE "
                + SESSIONS_TABLE + "(" + KEY_SESSIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_STARTTIME
                + " TEXT," + KEY_STARTDATE + " TEXT," + KEY_DURATION
                + " INTEGER," + KEY_DISTANCE + " REAL" + ")";
        //Locations table Create statement
       private static final String CREATE_LOCATIONS_TABLE = "CREATE TABLE " + LOCATIONS_TABLE
                + "(" + KEY_LOCATIONS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + KEY_LATITUDE + " TEXT,"
                + KEY_LONGITUDE + " TEXT," + " REAL," + KEY_SPEED + " REAL,"
                + KEY_LOCATIONS_SESSIONS_ID + " INTEGER, " + KEY_ELAPSED_TIME + " INTEGER" + ")";



        public DBHelper(Context context) {

        super(context, DATABASE_NAME, null, 1);
            final String MNAME = "DBHelper()";
            if( DBG ) Log.i(TAG, MNAME + "entering...");
            getDatabase();
            if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */

    @Override
        public void onCreate(SQLiteDatabase db) {
        final String MNAME = "onCreate()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
            db.execSQL(CREATE_SESSION_TABLE);
            db.execSQL(CREATE_LOCATIONS_TABLE);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
        }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        final String MNAME = "onUpgrade()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        db.execSQL("DROP TABLE IF EXISTS " + SESSIONS_TABLE);
        if( DBG ) Log.i(TAG, MNAME + "SessionTabelOne...");
        db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE);
        if( DBG ) Log.i(TAG, MNAME + "SessionTabelTwo...");

        onCreate(db);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    /**
     * Gets a SQLiteDatabase object.
     *
     * @return writeable Database
     */
    public SQLiteDatabase getDatabase() {
        myDataBase = this.getWritableDatabase();

        return myDataBase;
    }

    /**
     * Creates a WorkoutSession in db.
     *
     * @param session the WorkoutSession to be stored in the db
     * @return the id nr of the WorkoutSession
     */
    public long createWorkoutSession(WorkoutSession session) {
        final String MNAME = "createWorkoutSession()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        ContentValues values = new ContentValues();
        values.put(KEY_STARTTIME, session.getStartTime());
        values.put(KEY_STARTDATE, session.getStartDate());

        long sessionId = 0;
        if( DBG ) Log.i(TAG, MNAME + "before tray and catch...");
        try {
            //Inserts the WorkoutSession data in the db and returns an id
            sessionId = myDataBase.insert(SESSIONS_TABLE, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        if( DBG ) Log.i(TAG, MNAME + "afterthe catch ...");
        return sessionId;
    }

    /**
     * Creates a WorkoutLocation in db.
     *
     * @param sessionId   id nr of the WorkoutSession
     * @param latitude    latitude of the WorkoutLocation
     * @param longitude   longitude of the WorkoutLocation
     * @param v
     * @param speed       speed in m/s, when the WorkoutLocation was registered
     * @param elapsedTime elapsed time of the WorkoutSession when the WorkoutLocation was registered
     * @return the id nr of the WorkoutLocation
     */
    public long createWorkoutLocation(long sessionId, String latitude, String longitude,
                                      double v, float speed, long elapsedTime) {
        ContentValues values = new ContentValues();
        values.put(KEY_LOCATIONS_SESSIONS_ID, sessionId);
        values.put(KEY_LATITUDE, latitude);
        values.put(KEY_LONGITUDE, longitude);

        values.put(KEY_SPEED, speed);
        values.put(KEY_ELAPSED_TIME, elapsedTime);

        long id = 0;

        try {
            //Inserts the WorkoutLocation data in the db and returns an id
            id = myDataBase.insert(LOCATIONS_TABLE, null, values);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
        return id;
    }

    /**
     * Gets a list of the specific WorkoutSession´s WorkoutLocations from the db.
     *
     * @param sessionId id nr of the WorkoutSession
     * @return list of WorkoutLocations
     */
    public List<WorkoutLocation> getLocationsFromSession(long sessionId) {
        List<WorkoutLocation> locations = new ArrayList<>();

        //Selects all from Locations where session_id (foreign key)=sessionId
        String selectQuery = "SELECT  * FROM " + LOCATIONS_TABLE + " WHERE "
                + KEY_LOCATIONS_SESSIONS_ID + " = " + sessionId;

        try {
            Cursor c = myDataBase.rawQuery(selectQuery, null);

            //Loops through all rows, creates a WorkoutLocation object from the data, then adds it to the list
            if (c.moveToFirst()) {
                do {
                    WorkoutLocation location = new WorkoutLocation();
                    location.setId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_ID)));
                    location.setLatitude((c.getString(c.getColumnIndex(KEY_LATITUDE))));
                    location.setLongitude(c.getString(c.getColumnIndex(KEY_LONGITUDE)));
                    location.setSpeed(c.getFloat(c.getColumnIndex(KEY_SPEED)));
                    location.setElapsedTime(c.getLong(c.getColumnIndex(KEY_ELAPSED_TIME)));
                    location.setSessionId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_SESSIONS_ID)));

                    locations.add(location);
                } while (c.moveToNext());
            }
            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return locations;

    }

    /**
     * Gets a specific WorkoutSession
     *
     * @param sessionId id nr of the specific WorkoutSession
     * @return the WorkoutSession
     */
    public WorkoutSession getWorkoutSession(long sessionId) {
        WorkoutSession session = null;
        List<WorkoutLocation> locations = new ArrayList<>();

        //Selects all from sessions table where session_id=sessionId
        String selectQuery = "SELECT  * FROM " + SESSIONS_TABLE + " WHERE "
                + KEY_SESSIONS_ID + " = " + sessionId;

        try {
            Cursor c = myDataBase.rawQuery(selectQuery, null);

            if (c != null)
                c.moveToFirst();

            //Creates a WorkoutSession
            session = new WorkoutSession();
            session.setId(c.getInt(c.getColumnIndex(KEY_SESSIONS_ID)));
            session.setStartDate((c.getString(c.getColumnIndex(KEY_STARTDATE))));
            session.setStartTime(c.getString(c.getColumnIndex(KEY_STARTTIME)));
            session.setDistance((c.getFloat(c.getColumnIndex(KEY_DISTANCE))));
            session.setDuration(c.getLong(c.getColumnIndex(KEY_DURATION)));

            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        //Selects all from locations table where session_id (foreign key)=sessionId
        selectQuery = "SELECT  * FROM " + LOCATIONS_TABLE + " WHERE "
                + KEY_LOCATIONS_SESSIONS_ID + " = " + sessionId;

        try {
            Cursor c = myDataBase.rawQuery(selectQuery, null);

            //Loops through all rows, creates a WorkoutLocation from each row and adds it to the list of locations
            if (c.moveToFirst()) {
                do {

                    WorkoutLocation location = new WorkoutLocation();
                    location.setId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_ID)));
                    location.setLatitude((c.getString(c.getColumnIndex(KEY_LATITUDE))));
                    location.setLongitude(c.getString(c.getColumnIndex(KEY_LONGITUDE)));
                    location.setSpeed(c.getFloat(c.getColumnIndex(KEY_SPEED)));
                    location.setElapsedTime(c.getLong(c.getColumnIndex(KEY_ELAPSED_TIME)));
                    location.setSessionId(c.getInt(c.getColumnIndex(KEY_LOCATIONS_SESSIONS_ID)));

                    locations.add(location);
                } while (c.moveToNext());
            }

            session.setLocations(locations);

            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return session;
    }

    /**
     * Gets a list of all WorkoutSessions
     *
     * @return list of WorkoutSessions
     */
    public List<WorkoutSession> getAllWorkoutSessions() {
        List<WorkoutSession> sessions = new ArrayList<>();

        //Selects session_id from sessions table
        String selectQuery = "SELECT " + KEY_SESSIONS_ID + " FROM " + SESSIONS_TABLE;

        try {
            Cursor c = myDataBase.rawQuery(selectQuery, null);

            // Loops through all session_id´s and calls getWorkoutSession for each,
            // then adds the WorkoutSession to the list of WorkoutSessions
            if (c.moveToFirst()) {
                do {
                    WorkoutSession session = getWorkoutSession(c.getInt(c.getColumnIndex(KEY_SESSIONS_ID)));
                    sessions.add(session);
                } while (c.moveToNext());
            }
            c.close();
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return sessions;
    }


    /**
     * Deletes a specific WorkoutSession
     *
     * @param sessionId id nr of the WorkoutSession
     */
    public void deleteWorkoutSession(long sessionId) {
        try {
            //Deletes the WorkoutSession data from table sessions where session_id=sessionId
            int sessionNr = myDataBase.delete(SESSIONS_TABLE, KEY_SESSIONS_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});

            //Deletes all WorkoutLocations data from table locations where session_id(foreign key)=sessionId
            int locationsNr = myDataBase.delete(LOCATIONS_TABLE, KEY_LOCATIONS_SESSIONS_ID + " = ?",
                    new String[]{String.valueOf(sessionId)});
        } catch (SQLiteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates a WorkoutSession
     *
     * @param session the WorkoutSession to be updated
     * @return number of rows affected
     */
    public int updateWorkoutSession(WorkoutSession session) {
        ContentValues values = new ContentValues();
        values.put(KEY_DURATION, session.getDuration());
        values.put(KEY_DISTANCE, session.getDistance());

        int rowsAffected = 0;

        try {
            rowsAffected = myDataBase.update(SESSIONS_TABLE, values,
                    KEY_SESSIONS_ID + "=" + session.getId(), null);
        } catch (SQLiteException e) {
            e.printStackTrace();
        }

        return rowsAffected;
    }

    /**
     * Closes the database
     */
    public void closeDB() {
        if (myDataBase != null && myDataBase.isOpen())
            myDataBase.close();
    }
}
