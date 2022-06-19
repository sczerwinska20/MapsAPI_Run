package appentwicklung.android.mapsapi_run;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import appentwicklung.android.mapsapi_run.helper.DBHelper;
import appentwicklung.android.mapsapi_run.helper.HelperUtils;


public class DataTracking extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private final boolean DBG = true;//Zum Debuggen
    private static final String TAG = "DataTracking"; //Zum Debuggen, gibt Classe an


    private GoogleApiClient mGoogleApiClient;

    private LocationRequest mLocationRequest;

    private long mElapsedTime;

    private long mServiceStartTime;

    private long mSessionId;

    private DBHelper mDb;

    private static boolean runningService = false;

    @Override
    public void onCreate() {
        super.onCreate();
        final String MNAME = "oncreate()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");

        mDb = new DBHelper(this);

        createLocationRequest();

        //Creates the GoogleApiClient and connects it
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    /**
     * Creates a LocationRequest
     */
    protected void createLocationRequest() {
        final String MNAME = "createLocRequest()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        final String MNAME = "startComand()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        runningService = true;

        //Sets service start time in seconds
        mServiceStartTime = System.currentTimeMillis() / 1000;

        //Gets current session id from the intent
        mSessionId = Long.valueOf(intent.getExtras().getString("sessionId"));

        //Gets elapsed time from the intent
        mElapsedTime = Long.valueOf(intent.getExtras().getString("elapsedTime"));

        return Service.START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        final String MNAME = "onconnect()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        //If permission ACCESS_FINE_LOCATION is granted - gets the last location
                        //available and requests location updates
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            @SuppressLint("MissingPermission") Location startLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (startLocation != null) {
                                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, DataTracking.this);
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onLocationChanged(Location location) {
        final String MNAME = "on loc changed()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        long locId = saveLocationToDb(location);          //Saves the location to db

        //Sends a local broadcast to the RouteBroadCastReceiver in MainActivity
        Intent localBroadcastIntent = new Intent("com.daniel.workouttracker.TrackingService");
        localBroadcastIntent.putExtra("RESULT_CODE", "LOCAL");
        localBroadcastIntent.putExtra("sessionId", Long.toString(mSessionId));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localBroadcastIntent);
    }

    /**
     * Saves the users location (WorkoutLocation) to db
     *
     * @param location location of the user
     * @return id of the stored WorkoutLocation
     */
    public long saveLocationToDb(Location location) {
        final String MNAME = "saveLocationToDb()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        //Sets the elapsed time in service
        long elapsedTimeInService = (System.currentTimeMillis() / 1000) - mServiceStartTime;

        //Total elapsed time=elapsed time from MainActivity + elapsed time in service
        long totalElapsedTime = mElapsedTime + elapsedTimeInService;

        //Saves the WorkoutLocation to db
        long id = mDb.createWorkoutLocation(mSessionId, String.valueOf(location.getLatitude()),
                String.valueOf(location.getLongitude()), HelperUtils.formatDouble(location.getAltitude()),
                HelperUtils.convertSpeed(location.getSpeed()), totalElapsedTime);

        return id;
    }

    @Override
    public void onDestroy() {
        final String MNAME = "onDestroy()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        mGoogleApiClient.disconnect();
        Log.d("TrackingService", "TrackingService is being killed");
        runningService = false;
        super.onDestroy();
    }

    /**
     * Checks if the service is running
     *
     * @return boolean that shows the status of the service
     */
    public static boolean isServiceRunning() {
        return runningService;
    }
}

