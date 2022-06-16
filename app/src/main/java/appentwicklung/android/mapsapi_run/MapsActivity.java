package appentwicklung.android.mapsapi_run;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
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
 * MainActivity class
 *
 * @author Daniel Johansson
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private LocationListener locationListener;
    private GoogleMap mMap;
    private LatLng latLng;
    private LocationManager locManager;
    private Location location;

    /**
     * Last position of the user
     */
    private LatLng mLastPosition;

    /**
     * Permissions request flag
     */
    private final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    /**
     * Boolean for permission (ACCESS_FINE_LOCATION) granted status
     */
    private boolean mLocationPermissionGranted = false;

    /**
     * Total distance of the WorkoutSession
     */
    private float mTotalDistance;

    /**
     * Elapsed time of the WorkoutSession in seconds
     */
    private long mElapsedTime;

    /**
     * Distance TextView
     */
    private TextView mTextviewDistance;

    /**
     * Elapsed time TextView
     */
    private TextView mTextviewTime;

    /**
     * Start session Button
     */
    private Button mButtonStart;

    /**
     * Handler for the elapsed time timer
     */
    private Handler mTimerHandler;

    /**
     * Timer Runnable
     */
    private Runnable mTimerRunnable;

    /**
     * WorkoutSession
     */
    private WorkoutSession mWorkoutSession;

    /**
     * Session id of the WorkoutSession
     */
    private long mSessionId;

    /**
     * DBHelper
     */
    private DBHelper myDatabase;

    /**
     * RouteBroadCastReceiver
     */
    private RouteBroadCastReceiver mRouteReceiver;

    /**
     * LocationRequest
     */
    private LocationRequest mLocationRequest;
    private Object GoogleApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createLocationRequest();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mRouteReceiver = new RouteBroadCastReceiver();

        //If the TrackingService is running, stop it and set the SessionStatus to "readyToStart"
        if (DataActivity.isServiceRunning()) {
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

    }

    @Override
    public void onPause() {
        super.onPause();

        //If TrackingService isnt running - stop location updates
        if ( DataActivity.isServiceRunning() == false) {
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
        if ( DataActivity.isServiceRunning() == false) {
            startLocationUpdates();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

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
        SharedPreferences sharedPref = getSharedPreferences("com.daniel.workouttracker.PREFERENCES",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("sessionStatus", status);
        editor.commit();
    }

    /**
     * Gets the WorkoutSession´s status from SharedPreferences
     *
     * @return the status of the WorkoutSession
     */
    public String getSessionStatus() {
        SharedPreferences sharedPref = getSharedPreferences("com.daniel.workouttracker.PREFERENCES",
                Context.MODE_PRIVATE);
        String status = sharedPref.getString("sessionStatus", "");
        return status;
    }



    /**
     * Creates a LocationRequest that is set to the update interval of 5 seconds
     */
    public void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    //When the GoogleApi has connected
  /*  @Override
    public void onConnected(Bundle connectionHint) {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings();
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
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
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
        mMap = googleMap;
    }

    /**
     * Inits the GoogleMap with the device´s last known location and other settings
     */
    @SuppressWarnings({"MissingPermission", "deprecation"})
    public void initMap() {


         locationListener = new LocationListener() {


            @Override
            public void onLocationChanged(@androidx.annotation.NonNull Location location) {
                try {
                    latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(latLng).title("Position"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }

             LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        };


            try{locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 3, (android.location.LocationListener) locationListener);
            }
            catch(SecurityException e) {
                e.printStackTrace();

            }


    }

    /**
     * Starts location updates when a WorkoutSession hasn´t been started
     */

    public void startLocationUpdates() {
        try{
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.addMarker(new MarkerOptions().position(latLng).title("Position"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        } catch(SecurityException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        else{
            Toast.makeText(this, "no Location", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Updates the GoogleMap with a new location (only for when a WorkoutSession hasn´t been started)
     *
     * @param location location to update map with
     */
    public void updateLocationOnMap(Location location) {
        LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(17));
    }

    public void startWorkoutSession(View v) {
        startWorkoutSession();
    }

    /**
     * Starts the WorkoutSession
     */
    public void startWorkoutSession() {
        //When sessionStatus=="readyToStart" - Session has not been started before
        if (getSessionStatus().equals("readyToStart")) {
            setSessionStatus("started");

            String startTime = DateFormat.getTimeInstance().format(new Date());
            String startDate = DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(new Date());

            //Creates new WorkoutSession object and saves it to db
            mWorkoutSession = new WorkoutSession();
            mWorkoutSession.setStartTime(startTime);
            mWorkoutSession.setStartDate(startDate);
            mSessionId = myDatabase.createWorkoutSession(mWorkoutSession);        //Saves session to db
            mWorkoutSession.setId((int) mSessionId);

            mButtonStart.setText("Pause session");
            handleTimer();                                      //Handles the elapsed time timer
            startService();                                     //Starts the tracking service
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
        if (getSessionStatus().equals("started")) {
            mElapsedTime = 0;
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }
            };
            timerCount();
        } else if (getSessionStatus().equals("resumed")) {
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }
            };
            timerCount();
        } else
            mTimerHandler.removeCallbacks(mTimerRunnable);
    }

    /**
     * Increments mElapsedTime each second and sets the TextView
     */
    public void timerCount() {
        mElapsedTime++;
        mTextviewTime.setText(DateUtils.formatElapsedTime(mElapsedTime));
        mTimerHandler.postDelayed(mTimerRunnable, 1000);
    }

    public void stopWorkoutSession(View v) {
        stopWorkoutSession();
    }

    /**
     * Stops the WorkoutSession
     */
    public void stopWorkoutSession() {
        //If a WorkoutSession has been started, paused or resumed - stop the WorkoutSession
        if (getSessionStatus().equals("started") || getSessionStatus().equals("resumed")
                || getSessionStatus().equals("paused")) {
            stopService();

            //Sets the WorkoutSession status to "readyToStart" (so that new sessions can be started)
            setSessionStatus("readyToStart");
            mButtonStart.setText("Start session");

            mTimerHandler.removeCallbacks(mTimerRunnable);

            updateSessionInDb();

            drawEndMarker();            //Draws the end marker on the map

            startLocationUpdates();     //Starts location updates (without a active WorkoutSession)
        }
        //If no WorkoutSession is active - show Toast message
        else
            Toast.makeText(this, "No workout session active!", Toast.LENGTH_SHORT).show();
    }

    /**
     * Updates the WorkoutSession in the database
     */
    public void updateSessionInDb() {
        //Saves the last location to the database
        saveLastLocationToDb();

        mWorkoutSession.setDuration(mElapsedTime);

        //Formats and rounds the distance to 1 decimal
        float formattedDistance = HelperUtils.formatFloat(mTotalDistance);
        mWorkoutSession.setDistance(formattedDistance);

        //Updates the WorkoutSession in db with duration and distance
        myDatabase.updateWorkoutSession(mWorkoutSession);

        //Gets the list of locations from db and adds it to the WorkoutSession
        List<WorkoutLocation> locations = myDatabase.getLocationsFromSession(mSessionId);
        mWorkoutSession.setLocations(locations);
    }

    /**
     * Saves the last location of the WorkoutSession to the database
     */
    @SuppressWarnings({"MissingPermission"})
    public void saveLastLocationToDb() {

       // Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApi);
        LatLng endPosition = new LatLng(location.getLatitude(),
                location.getLongitude());

        myDatabase.createWorkoutLocation(mSessionId, String.valueOf(location.getLatitude()),
                String.valueOf(location.getLongitude()), HelperUtils.formatDouble(location.getAltitude()),
                HelperUtils.convertSpeed(location.getSpeed()), mElapsedTime);
    }

    /**
     * Starts the TrackingService
     */
    public void startService() {
        Intent serviceIntent = new Intent(this, DataActivity.class);
        serviceIntent.putExtra("sessionId", Long.toString(mSessionId));
        serviceIntent.putExtra("elapsedTime", Long.toString(mElapsedTime));

        this.startService(serviceIntent);
    }

    /**
     * Stops the TrackingService
     */
    public void stopService() {
        Intent stopServiceIntent = new Intent(MapsActivity.this, DataActivity.class);
        stopService(stopServiceIntent);
    }

    /**
     * Clears the GoogleMap and the TextViews in the activity
     */
    public void clearSession() {
        mMap.clear();
        mTextviewTime.setText("");
        mTextviewDistance.setText("");
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
     * Class for receiving location broadcasts from TrackingService
     */
    private class RouteBroadCastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("appentwicklung.android.mapsapi_run. Pakcge.DataActivity")) {
                long sessionId = Long.parseLong(intent.getExtras().getString("sessionId"));

                WorkoutSession session = myDatabase.getWorkoutSession(sessionId);
                List<WorkoutLocation> locations = session.getLocations();

                if (locations.size() > 0) {
                    //Gets all LatLng points from the list of WorkoutLocations
                    List<LatLng> points = getPoints(locations);
                    mMap.clear();

                    drawRoute(points);
                    showDistance(points);

                }
            }
        }
    }
    /**
     * Class for splash screen activity
     *
     * @author Daniel Johansson
     */
    public class SplashScreen extends AppCompatActivity {

        /** Splash screen timeout */
        private  int SPLASH_TIME_OUT = 2000;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_log);

            //Starts the Main activity after the SPLASH_TIME_OUT
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(SplashScreen.this, MainActivity.class);
                    startActivity(i);

                    finish();
                }
            }, SPLASH_TIME_OUT);
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
        polyOptions.color(Color.RED);
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

        mLastPosition = points.get(points.size() - 1);
        CameraUpdate cUpdate = CameraUpdateFactory.newLatLng(mLastPosition);
        mMap.animateCamera(cUpdate);

    }

    /**
     * Draws the end marker
     */
    public void drawEndMarker() {
        mMap.addMarker(new MarkerOptions()
                .position(mLastPosition)
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

    /**
     * Shows the total distance of the WorkoutSession in the TextView
     *
     * @param points list of LatLngs
     */
    public void showDistance(List<LatLng> points) {
        mTotalDistance = calculateDistance(points);
        float displayDistance = mTotalDistance / 1000;       //Converts metres to km
        mTextviewDistance.setText(Float.toString(displayDistance) + " km");

        //Locale used for "." in formatting of String
        mTextviewDistance.setText(String.format(java.util.Locale.US, "%.2f", displayDistance) + " km");
    }

}

