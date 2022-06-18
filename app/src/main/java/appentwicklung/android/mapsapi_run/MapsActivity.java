package appentwicklung.android.mapsapi_run;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import appentwicklung.android.mapsapi_run.helper.DBHelper;
import appentwicklung.android.mapsapi_run.helper.HelperUtils;
import appentwicklung.android.mapsapi_run.model.WorkoutLocation;
import appentwicklung.android.mapsapi_run.model.WorkoutSession;

/**
 * Die Klasse MapsActivity steuert die Hauptfunktionalität der App.
 * Sie beinhaltete die Karte, so wie eine Vielzhal ebnötigter Methoden
 * um die Mainfunktionalität abzudecken.
 * Gleichzeiting Kommuniztiert die mit der Datenbank und speichert die Sessions
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener,GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private final boolean DBG = true;//Zum Debuggen
    private static final String TAG = "MapsActiviry"; //Zum Debuggen, gibt Classe an
    private GoogleMap mMap;
    private LatLng latLng;
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted = false;
    private float mTotalDistance;
    private long mElapsedTime;
    private TextView mTextviewTime;
    private Handler mTimerHandler;
    private Runnable mTimerRunnable;
    private WorkoutSession mWorkoutSession;
    private long mSessionId;
    private DBHelper myDatabase;
    private RouteBroadCastReceiver mRouteReceiver;
    private LocationRequest myLocRequest;
    private Button mButtonStart;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final String MNAME = "onCreate()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
        if( DBG ) Log.i(TAG, MNAME + "SuportMap Fragment...");

        //startUi();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mButtonStart = findViewById(R.id.start_pause);
        //Creates the GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();
        if( DBG ) Log.i(TAG, MNAME + "Location Requested...");
        mRouteReceiver = new RouteBroadCastReceiver();

        //If the TrackingService in Data Activity  is running, stop it and set the SessionStatus to "readyToStart"
        if (DataTracking.isServiceRunning()) {
            stopService();
            setSessionStatus("readyToStart");
        }
        //If the DataActivity is not running, set the SessionStatus to "readyToStart"
        else {
            setSessionStatus("readyToStart");
        }

        myDatabase = new DBHelper(this.getApplicationContext());
    }



    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();

        //If TrackingService isnt running - stop location updates
        if (mGoogleApiClient.isConnected() && DataTracking.isServiceRunning() == false) {
            stopLocationUpdates();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRouteReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (myDatabase == null)
            myDatabase = new DBHelper(this.getApplicationContext());

        if (mRouteReceiver == null) {
            mRouteReceiver = new RouteBroadCastReceiver();
        }

        //Registers the RouteBroadcastReceiver
        IntentFilter filter = new IntentFilter(" appentwicklung.android.mapsapi_run.DataActivity");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRouteReceiver, filter);

        //If TrackingService isnt running - start location updates that arent part of the session
        if (mGoogleApiClient.isConnected() && !DataTracking.isServiceRunning()) {
            startLocationUpdates();
        }
    }
    @Override
    public void onStop() {
        final String MNAME = "onStop()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopService();

        myDatabase.closeDB();
        myDatabase = null;
    }

    /**
     * Sets the WorkoutSession´s status in SharedPreferences
     *
     * @param status status of the WorkoutSession. "readyToStart", "started" or "paused".
     */
    public void setSessionStatus(String status) {
        SharedPreferences sharedPref = getSharedPreferences("appentwicklung.android.mapsapi_run.PREFERENCES",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("sessionStatus", status);
        editor.apply();
    }

    /**
     * Gets the WorkoutSession´s status from SharedPreferences
     *
     * @return the status of the WorkoutSession
     */
    public String getSessionStatus() {
        final String MNAME = "getSessionStatus()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        SharedPreferences sharedPref = getSharedPreferences("appentwicklung.android.mapsapi_run.PREFERENCES",
                Context.MODE_PRIVATE);
        String status = sharedPref.getString("sessionStatus", "");
        if( DBG ) Log.i(TAG, MNAME + "exiting...");
        return status;

    }
    public void onClickStop(View view){
        final String MNAME = "onClickStop()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        onStop();

        Intent intent =new Intent();
        intent.setClass(getApplicationContext(), DisplayDataActivity.class);
        setContentView(R.layout.activity_data);
        startActivity(intent);
        if( DBG ) Log.i(TAG, MNAME + "exiting...");

    }

    /**
     * Creates a LocationRequest that is set to the update interval of 5 seconds
     */
    public void createLocationRequest() {
        final String MNAME = "createLocationRequest()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        myLocRequest = new LocationRequest();
        myLocRequest.setInterval(5000);
        myLocRequest.setFastestInterval(5000);
        myLocRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("google_connection", "Play services connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("google_connection", "Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }
    //When the GoogleApi has connected
    @Override
    public void onConnected(Bundle connectionHint) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(myLocRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        //If permission ACCESS_FINE_LOCATION is granted, sets mLocationPermissionGranted
                        //and calls the initMap method
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = true;

                            initMap();

                            //If permission ACCESS_FINE_LOCATION is not granted, request permission from user
                        } else {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }

                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,  String permissions[], int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {

                //If permission ACCESS_FINE_LOCATION is granted, sets mLocationPermissionGranted
                //and calls the initMap method
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    initMap();
                }
                //If the user refuses the permission request, the app is finished
                else {
                    Toast.makeText(this, "Permission not granted. Exiting app!", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        initMap();
        mMap = googleMap;

          LatLng berlin = new LatLng(52.56, 13.37);
        mMap.addMarker(new MarkerOptions()
                .position(berlin)
                .title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(berlin));
    }

    /**
     * Inits the GoogleMap with the device´s last known location and other settings
     */
    @SuppressWarnings({"MissingPermission"})
    public void initMap() {

        if (mLocationPermissionGranted) {
            Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (lastLocation != null) {
                LatLng position = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                mMap.clear();
                mMap.resetMinMaxZoomPreference();

                mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                mMap.getUiSettings().setZoomControlsEnabled(true);
            }else {
                Toast.makeText(this, "Location not Found", Toast.LENGTH_SHORT).show();
        }
        }
    }

    /**
     * Starts location updates when a WorkoutSession hasn´t been started
     */

    @SuppressWarnings({"MissingPermission"})
    public void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, myLocRequest, this);
    }

    /**
     * Stops location updates (for when a WorkoutSession hasn´t been started)
     */
    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        updateLocationOnMap(location);
        /* if (location != null){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        else{
            Toast.makeText(this, "no Location", Toast.LENGTH_SHORT).show();
        }*/

    }

    /**
     * Updates the GoogleMap with a new location (only for when a WorkoutSession hasn´t been started)
     *
     * @param location location to update map with
     */
    public void updateLocationOnMap(Location location) {
        final String MNAME = "updateLocationOnMap()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));

        if( DBG ) Log.i(TAG, MNAME + "exeting, got new location...");
    }

    public void startWorkoutSession(View v) {
        startWorkoutSession();
    }

    /**
     * Starts the WorkoutSession
     */
    public void startWorkoutSession() {
        final String MNAME = "startWorkoutSession()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        //When sessionStatus=="readyToStart" - Session has not been started before
        if (getSessionStatus().equals("readyToStart")) {
            setSessionStatus("started");

            stopLocationUpdates();

            String startTime = DateFormat.getTimeInstance().format(new Date());
            String startDate = DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(new Date());

            //Creates new WorkoutSession object and saves it to db
            mWorkoutSession = new WorkoutSession();
            mWorkoutSession.setStartTime(startTime);
            mWorkoutSession.setStartDate(startDate);
            mSessionId = myDatabase.createWorkoutSession(mWorkoutSession);        //Saves session to db
            mWorkoutSession.setId((int) mSessionId);

            mButtonStart.setText("Pause session");

            handleTimer();
            //Handles the elapsed time timer
            startService();//Starts the tracking service
            if( DBG ) Log.i(TAG, MNAME + "exiting...");
        }

        //When sessionStatus=="paused - Session is paused and will be resumed
        //Timer for mElapsedTime is also restarted
        else if (getSessionStatus().equals("paused")) {
            setSessionStatus("resumed");
            mButtonStart.setText("Pause session");
            handleTimer();                                       //Handles the elapsed time timer
            startService();                                     //Starts the tracking service
        }

        //When sessionStatus=="started" or "resumed" - Session will be paused
        //The mElapsedTime timer is stopped
        else if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")) {
            setSessionStatus("paused");
            mButtonStart.setText("Resume session");
            handleTimer();                                      //Handles the elapsed time timer
            stopService();                                      //Starts the tracking service
        }
    }

    /**
     * Handles the elapsed time timer
     */
    public void handleTimer() {
        final String MNAME = "handleTimer()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");

        if (getSessionStatus().equals("started")) {
            if( DBG ) Log.i(TAG, MNAME + "1...");
            mElapsedTime = 0;
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }
            };
            if( DBG ) Log.i(TAG, MNAME + "2...");
            timerCount();
            if( DBG ) Log.i(TAG, MNAME + "3...");
        }
        else if (getSessionStatus().equals("resumed")) {
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }

            };
            if( DBG ) Log.i(TAG, MNAME + "4...");
            timerCount();
            if( DBG ) Log.i(TAG, MNAME + "5...");
        } else
            mTimerHandler.removeCallbacks(mTimerRunnable);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    /**
     * Increments mElapsedTime each second and sets the TextView
     */
    public void timerCount() {
        final String MNAME = "timerCount()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");

        mElapsedTime++;
        if( DBG ) Log.i(TAG, MNAME + "1...");
      //  mTextviewTime.setText(DateUtils.formatElapsedTime(mElapsedTime));
        if( DBG ) Log.i(TAG, MNAME + "2...");
        mTimerHandler.postDelayed(mTimerRunnable, 1000);
        if( DBG ) Log.i(TAG, MNAME + "exetiinhg...");
    }

    public void stopWorkoutSession(View v) {
        stopWorkoutSession();
    }

    public void stopWorkoutSession() {
        final String MNAME = "stopWorkoutSession()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        //If a WorkoutSession has been started, paused or resumed - stop the WorkoutSession
        if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")
                || getSessionStatus().equals("paused")) {
            stopService();
            //Sets the WorkoutSession status to "readyToStart" (so that new sessions can be started)
            setSessionStatus("readyToStart");
            mButtonStart.setText("Start session");
            mTimerHandler.removeCallbacks(mTimerRunnable);
            if( DBG ) Log.i(TAG, MNAME + "stoped session...");
            updateSessionInDb();
            if( DBG ) Log.i(TAG, MNAME + "updated session in db...");
            drawEndMarker();            //Draws the end marker on the map
            if( DBG ) Log.i(TAG, MNAME + "drew the endmarker...");
            startLocationUpdates();//Starts location updates (without a active WorkoutSession)
            if( DBG ) Log.i(TAG, MNAME + "restarts the locationupdates...");
        }
        //If no WorkoutSession is active - show Toast message
        else
            Toast.makeText(this, "No workout session active!", Toast.LENGTH_SHORT).show();
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }




    public void updateSessionInDb() {
        final String MNAME = "updateSessionInDb()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        //Saves the last location to the database
        saveLastLocationToDb();
        if( DBG ) Log.i(TAG, MNAME + "Saved Session to Db...");
        mWorkoutSession.setDuration(mElapsedTime);

        //Formats and rounds the distance to 1 decimal
        float formattedDistance = HelperUtils.formatFloat(mTotalDistance);
        mWorkoutSession.setDistance(formattedDistance);

        //Updates the WorkoutSession in db with duration and distance
        myDatabase.updateWorkoutSession(mWorkoutSession);
        if( DBG ) Log.i(TAG, MNAME + "saves List of the Locations...");
        //Gets the list of locations from db and adds it to the WorkoutSession
        List<WorkoutLocation> locations = myDatabase.getLocationsFromSession(mSessionId);
        mWorkoutSession.setLocations(locations);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    /**
     * Saves the last location of the WorkoutSession to the database
     */
    @SuppressWarnings({"MissingPermission"})
    public void saveLastLocationToDb() {
        final String MNAME = "saveLastLocationToDb()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng endPosition = new LatLng(lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude());
        if( DBG ) Log.i(TAG, MNAME + "got the last known position...");
        myDatabase.createWorkoutLocation(mSessionId, String.valueOf(lastKnownLocation.getLatitude()),
                String.valueOf(lastKnownLocation.getLongitude()), HelperUtils.formatDouble(lastKnownLocation.getAltitude()),
                HelperUtils.convertSpeed(lastKnownLocation.getSpeed()), mElapsedTime);
        if( DBG ) Log.i(TAG, MNAME + "exeting after caculating the needes datas...");
    }

    /**
     * Starts the TrackingService
     */
    public void startService() {
        final String MNAME = "startService()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        Intent serviceIntent = new Intent(this, DataTracking.class);
        serviceIntent.putExtra("sessionId", Long.toString(mSessionId));
        serviceIntent.putExtra("elapsedTime", Long.toString(mElapsedTime));
        if( DBG ) Log.i(TAG, MNAME + "started Data tracking...");
        this.startService(serviceIntent);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    /**
     * Stops the TrackingService
     */
    public void stopService() {
        final String MNAME = "stopService()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        Intent stopServiceIntent = new Intent(MapsActivity.this, DataTracking.class);
        stopService(stopServiceIntent);
        if( DBG ) Log.i(TAG, MNAME + "exeting...");
    }

    /**
     * Starts the ViewSessionDetailsActivity
     */
    public void startViewSessionDetails() {
        Intent intent = new Intent(this, DisplayDataActivity.class);
        intent.putExtra("session", mWorkoutSession);

        //Calculates average speed (in km/h) and puts it as an Extra in the intent
        intent.putExtra("averageSpeed", HelperUtils.calculateAverageSpeed(mWorkoutSession));
        startActivity(intent);
    }


    /**
     * Class for receiving location broadcasts from DataTracking
     */
    private class RouteBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("appentwicklung.android.mapsapi_run.DataActivity")) {
                long sessionId = Long.parseLong(intent.getExtras().getString("sessionId"));

                WorkoutSession session = myDatabase.getWorkoutSession(sessionId);
                List<WorkoutLocation> locations = session.getLocations();

                if (locations.size() > 0) {
                    //Gets all LatLng points from the list of WorkoutLocations
                    List<LatLng> points = getPoints(locations);
                    mMap.clear();

                    drawRoute(points);
                }
            }
        }
    }


    /**
     * Gets a list of LatLng´s from the @param
     *
     * @param locations list of WorkoutLocations
     * @return list of LatLng
     */
    public List<LatLng> getPoints(List<WorkoutLocation> locations) {
        List<LatLng> points = new ArrayList<>();

        for (WorkoutLocation location : locations)
            points.add(new LatLng(Double.parseDouble(location.getLatitude()),
                    Double.parseDouble(location.getLongitude())));

        return points;
    }

    /**
     * Draws a polyline between all locations in the WorkoutSession, and adds a marker for the
     * start position
     *
     * @param points list of LatLng objects
     */
    public void drawRoute(List<LatLng> points) {
        PolylineOptions polyOptions = new PolylineOptions();
        polyOptions.color(Color.BLACK);
        polyOptions.width(5);
        polyOptions.geodesic(true);

        //Adds a marker for the starting position
        LatLng startPosition = points.get(0);
        mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("START"));

        //Draws a polyline between all locations in the list
        polyOptions.addAll(points);
        mMap.addPolyline(polyOptions);

        latLng = points.get(points.size() - 1);
        CameraUpdate cUpdate = CameraUpdateFactory.newLatLng(latLng);
        mMap.animateCamera(cUpdate);

    }

    /**
     * Draws the end marker
     */
    public void drawEndMarker() {
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("STOP"));
    }

    /**
     * Calculates the total distance between all locations
     *
     * @param points list of LatLngs
     * @return total distance
     */
    public float calculateDistance(List<LatLng> points) {
        float distance = 0;

        for (int i = 1; i < points.size(); i++) {
            float[] results = new float[2];
            Location.distanceBetween(points.get(i - 1).latitude, points.get(i - 1).longitude,
                    points.get(i).latitude, points.get(i).longitude, results);
            distance += results[0];
        }
        return distance;
    }

}