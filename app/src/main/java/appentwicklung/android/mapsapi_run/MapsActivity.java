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
    private boolean mLocationPermissionGranted = true;
    private float mTotalDistance;
    private long mElapsedTime;
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

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
//lädt Map-Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
//Sorgt für aktivierten Bildschirm während der App nutzung
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//Inizalisierung des StartButtons
        mButtonStart = findViewById(R.id.start_pause);
//Erzeugt die Verbindung zum GoogleApiClient-> braucht API Schlüssel(Manifest,Gradle)
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();

        createLocationRequest();
//ruft innere Klasse RouteBroadCastReceiver() auf
// Kommuniziert mit der Klasse DataTracking zum erzeigen einer Liste zum Speichern der Locations
        mRouteReceiver = new RouteBroadCastReceiver();

        //Wenn  DataTracking läuft wird er gestopt und der Status wird  auf  "readyToStart" gesetzt
        if (DataTracking.isServiceRunning()==true) {
            stopService();
            setSessionStatus("readyToStart");
        }
        //Wenn DataTracking  nicht läuft wird ebenfalls der Status  auf  "readyToStart" gesetzt
        else {
            setSessionStatus("readyToStart");
        }

        //Verbindung zur Datenbank wird aufgebaut
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

        //Wenn DataTrcking nicht läuft werden keien Locations gespeichert
        if (mGoogleApiClient.isConnected() && DataTracking.isServiceRunning() == false) {
            stopLocationUpdates();
        }
        //sendet Nachricht an innere Klasse RouteBroadCastReceiver()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRouteReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
//Setzt Session fort
        if (myDatabase == null)
            myDatabase = new DBHelper(this.getApplicationContext());

        if (mRouteReceiver == null) {
            mRouteReceiver = new RouteBroadCastReceiver();
        }

        //regesterite den RouteBroadcastReceiver->
        IntentFilter filter = new IntentFilter(" appentwicklung.android.mapsapi_run.DataActivity");
        LocalBroadcastManager.getInstance(this).registerReceiver(mRouteReceiver, filter);

        //Wenn DataTracking nicht läuft, wird die Location aktualisiert jedoch nicht gespeichert
        if (mGoogleApiClient.isConnected() && !DataTracking.isServiceRunning()) {
            startLocationUpdates();
        }
    }
    @Override
    public void onStop() {

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

    /**Setzt den WorkoutSession´s status in Shared Prefernces
     * auf entweder  "readyToStart", "started" oder "paused".
     */
    public void setSessionStatus(String status) {
        SharedPreferences sharedPref = getSharedPreferences("appentwicklung.android.mapsapi_run.PREFERENCES",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("sessionStatus", status);
        editor.apply();
    }
//ruft Status ab
    public String getSessionStatus() {

        SharedPreferences sharedPref = getSharedPreferences("appentwicklung.android.mapsapi_run.PREFERENCES",
                Context.MODE_PRIVATE);
        String status = sharedPref.getString("sessionStatus", "");

        return status;

    }
// On Click Listener: zum Stopen der Session, wird nach Abruf des Beenden Buttons ausgeführt
    public void onClickStop(View view){
//Wenn Data Tracking läuft wird der Service unterbrochen
        if(DataTracking.isServiceRunning() == true){

        stopWorkoutSession();
        }else { // sonst gestopt
            onStop();
        }
//activity Main wird wieder aufgerufen, und die MainActivity übernimmt erneut die Steuerung
        Intent intent =new Intent();
        intent.setClass(getApplicationContext(), MainActivity.class);
        setContentView(R.layout.activity_main);
        startActivity(intent);

    }

    /**
     * Funktion erstellt den benötigten LocationRequest alle 3 Sekunden
     */
    public void createLocationRequest() {

        myLocRequest = new LocationRequest();
        myLocRequest.setInterval(3000);
        myLocRequest.setFastestInterval(3000);
        myLocRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }
// Autogeneriert, gibt Nachricht aus
    @Override
    public void onConnectionSuspended(int i) {
        Log.d("google_connection", "Play services connection suspended");
    }
    // Autogeneriert, gibt Nachricht aus
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d("google_connection", "Play services connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());
    }

    /**
     * wenn die GoogleApi sich verbunden hat, wird folgende Methode abgerufen, sie erstellt einen Location Request,
     * überprüft den Erfolg, und initalisiert die Map
     * Falls das nicht möglich ist wird die Permission abgefragt
     *
     */

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

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = true;

                            initMap();

                            //Wenn die Zustimmung zu: ACCESS_FINE_LOCATION noch nicht erfolgt ist,
                            // wird die Zusimmung abgefragt
                        } else {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                        break;
                        //Andernfalls ist eine änderung nicht möglich, case fängt diese option ab
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    /**
     *Methode zum erzeugen der Map, mit Default auf Berlin Locations
     *Fragt Locations ab mit der Methode createLocationRequest()
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        initMap();
        mMap = googleMap;

          LatLng berlin = new LatLng(52.56, 13.37);
        mMap.addMarker(new MarkerOptions()
                .position(berlin)
                .title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(berlin));

            createLocationRequest();

    }

    /**
     * initalisiert die Map, mit der Position welche das Gerät als leztes hatte
     * wenn keine letzte Location vorliegt wird versucht selber eine zu finden
     * Einstellung zur Ansicht der Map (wie LcoationButton, Plus-Minus-operator) werden erzeugt
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
     * Erzeugt Location updates bevor die Workout Session begonnen hat
     */

    @SuppressWarnings({"MissingPermission"})
    public void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, myLocRequest, this);
    }

    /**
     * unterbricht die LocationUpdates
     */
    public void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    /**
     * Autogeneriert, Fragt Location ab, wenn möglich wird diese ausgegeben, wenn nicht Toast-Nachricht
     */
    @Override
    public void onLocationChanged(Location location) {
        updateLocationOnMap(location);
         if (location != null){
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        }
        else{
            Toast.makeText(this, "no Location", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * Aktualisert die Map, mit neuen Locations, wenn die Session noch nicht gestartet wurde
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
     * Wird abgerufen wenn die Workout Session über den Button mButtonStart bestarte wurde
     */
    public void startWorkoutSession() {

        //Wenn sessionStatus=="readyToStart" -Läuft nicht, dann keine Loc updtes
        if (getSessionStatus().equals("readyToStart")) {
            setSessionStatus("started");
//hier wird die Klasse Datatracking beansprucht
            stopLocationUpdates();

            String startTime = DateFormat.getTimeInstance().format(new Date());
            String startDate = DateFormat.getDateInstance(DateFormat.LONG, Locale.US).format(new Date());

            //erzeugt neue WorkoutSession objekt und speichert es in der DB
            mWorkoutSession = new WorkoutSession();
            mWorkoutSession.setStartTime(startTime);
            mWorkoutSession.setStartDate(startDate);
            mSessionId = myDatabase.createWorkoutSession(mWorkoutSession);
            mWorkoutSession.setId((int) mSessionId);

            mButtonStart.setText("Pause session");

            handleTimer();
            //händelt den Timer, stopt zeitaufnahme
            startService();//startet Data Tracking
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
     * händelt den Timer
     */
    public void handleTimer() {
//Wenn session gestartet: wird ein neuer Timer gestartet
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
        } // Wenn Session pausiert, wird der Timer pausiert
        else if (getSessionStatus().equals("resumed")) {
            mTimerHandler = new Handler();
            mTimerRunnable = new Runnable() {
                @Override
                public void run() {
                    timerCount();
                }

            };

            timerCount();

        } else // sonst wird der Timer entfernt
            mTimerHandler.removeCallbacks(mTimerRunnable);
    }

    /**
     * erzeugt vergangene Zeit (mElapsed Time) mit einer Sekunde verzögerung
     */
    public void timerCount() {

        mElapsedTime++;

        mTimerHandler.postDelayed(mTimerRunnable, 1000);
    }

    public void stopWorkoutSession(View v) {
        stopWorkoutSession();
    }

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

            startLocationUpdates();//Starts location updates (without a active WorkoutSession)
        }
        //If no WorkoutSession is active - show Toast message
        else
            Toast.makeText(this, "No workout session active!", Toast.LENGTH_SHORT).show();
    }

    /*
    Updated Session un DB
     */
    public void updateSessionInDb() {

        //Speichert die letzte Location in der Datenbank
        saveLastLocationToDb();
        //Setzt verstrichene zeit in der DB
        mWorkoutSession.setDuration(mElapsedTime);

        //Formatiert und rundet die Distanz auf eine nachKommaStelle
        float formattedDistance = HelperUtils.formatFloat(mTotalDistance);
        mWorkoutSession.setDistance(formattedDistance);

        //Aktualisiert die WorkoutSession in der DB mit dauer und distanz
        myDatabase.updateWorkoutSession(mWorkoutSession);

        // holt eine Liste der Locations  aud ser Datenbank fügt dies zur Klasse WorkoutSession hinzu
        List<WorkoutLocation> locations = myDatabase.getLocationsFromSession(mSessionId);
        mWorkoutSession.setLocations(locations);
    }

    /**
     * Funktion speichert die letzte Location in der DB
     */
    @SuppressWarnings({"MissingPermission"})
    public void saveLastLocationToDb() {
        // ruft letzet bekannte Location ab
        Location lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        LatLng endPosition = new LatLng(lastKnownLocation.getLatitude(),
                lastKnownLocation.getLongitude());
// speichert diese in der DB
        myDatabase.createWorkoutLocation(mSessionId, String.valueOf(lastKnownLocation.getLatitude()),
                String.valueOf(lastKnownLocation.getLongitude()), HelperUtils.formatDouble(lastKnownLocation.getAltitude()),
                HelperUtils.convertSpeed(lastKnownLocation.getSpeed()), mElapsedTime);

    }

    /**
     * Startet DataTracking
     */
    public void startService() {

        Intent serviceIntent = new Intent(this, DataTracking.class);
        serviceIntent.putExtra("sessionId", Long.toString(mSessionId));
        serviceIntent.putExtra("elapsedTime", Long.toString(mElapsedTime));

        this.startService(serviceIntent);
    }

    /**
     * Stopt DataTracking
     */
    public void stopService() {

        Intent stopServiceIntent = new Intent(MapsActivity.this, DataTracking.class);
        stopService(stopServiceIntent);

    }

    /**
     * Klasse zum Erhalten der Updates aus der Klasse DataActivity
     */
    private class RouteBroadCastReceiver extends BroadcastReceiver {

        private final boolean DBG = true;//Zum Debuggen
        private static final String TAG = "BroadcastReceiver"; //Zum Debuggen, gibt Classe a

        @Override
        public void onReceive(Context context, Intent intent) {
            final String MNAME = "onRecive()";
            if( DBG ) Log.i(TAG, MNAME + "entering...");

            if (intent.getAction().equals("appentwicklung.android.mapsapi_run.DataTracking")) {
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
            if( DBG ) Log.i(TAG, MNAME + "exeting...");
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
     * Zeichnet die Lini aller bisheringen aufgezeichneten Punkte der Aktuellen Session,
     * die der Nutzer bereits hinter sich gelassen hat.
     * Auch wird in dieser Funktion ein Marker auf den Startpunkt gesetzte
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
     * Funktion zum setzten des Endpunkts auf der Karte
     */
    public void drawEndMarker() {
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("STOP"));
    }
}