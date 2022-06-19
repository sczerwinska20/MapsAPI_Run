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
    private DBHelper myDataBase;
    private static boolean runningService = false;

    @Override
    public void onCreate() {
        super.onCreate();
// DB wird inizalisiert
        myDataBase = new DBHelper(this);
//Location update wird erzeugt
        createLocationRequest();

//erzeugt die Verbindung zum GoogleApiClient
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
     * Erzeugt LocationRequest
     * Alle 3 sekunden
     */
    protected void createLocationRequest() {
        final String MNAME = "createLocRequest()";
        if( DBG ) Log.i(TAG, MNAME + "entering...");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
// setzt den RunningService auf true
        runningService = true;

        // setzten die service startzeit, in Sekunden
        mServiceStartTime = System.currentTimeMillis() / 1000;

        //holt sich aktuelle Session Id vom Intent
        mSessionId = Long.valueOf(intent.getExtras().getString("sessionId"));

        //holt sich versrichene Zeit vom  intent
        mElapsedTime = Long.valueOf(intent.getExtras().getString("elapsedTime"));

        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
     /**
     * wenn die GoogleApi sich verbunden hat, wird folgende Methode abgerufen, sie erstellt einen Location Request,
     * überprüft ob permisson gegeben
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest);
        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onResult(@NonNull LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            @SuppressLint("MissingPermission") Location startLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                            if (startLocation != null) {
                                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, DataTracking.this);
                            }
                        }//Andernfalls ist eine änderung nicht möglich, case fängt diese option ab
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
        long locId = saveLocationToDb(location);          //Speichert die Location in der DB

        //Sendet eine Broadcast NAchricht zum  the RouteBroadCastReceiver in MapsActivity
        Intent localBroadcastIntent = new Intent("appentwicklung.android.mapsapi_run.DataTracking");
        localBroadcastIntent.putExtra("RESULT_CODE", "LOCAL");
        localBroadcastIntent.putExtra("sessionId", Long.toString(mSessionId));
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(localBroadcastIntent);
    }

    /**
     *Speichert die Location in die Db
     */
    public long saveLocationToDb(Location location) {

        //Setzt die vergangene Zeit in Service
        long elapsedTimeInService = (System.currentTimeMillis() / 1000) - mServiceStartTime;

        // insgesamt vergangene Zeit aus er MAps Activity + vergangene Zeit in Service
        long totalElapsedTime = mElapsedTime + elapsedTimeInService;

        //speichert die Workout Location in der DB
        long id = myDataBase.createWorkoutLocation(mSessionId, String.valueOf(location.getLatitude()),
                String.valueOf(location.getLongitude()), HelperUtils.formatDouble(location.getAltitude()),
                HelperUtils.convertSpeed(location.getSpeed()), totalElapsedTime);
// gibt Workout id zurück
        return id;
    }

    @Override
    public void onDestroy() {

        mGoogleApiClient.disconnect();
        Log.d("TrackingService", "TrackingService is being killed");
        runningService = false;
        super.onDestroy();
    }

    /**
     Checkt ob der Service läuft
     */
    public static boolean isServiceRunning() {
        return runningService;
    }
}

