package appentwicklung.android.mapsapi_run;

import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;

import appentwicklung.android.mapsapi_run.helper.HelperUtils;
import appentwicklung.android.mapsapi_run.model.WorkoutLocation;
import appentwicklung.android.mapsapi_run.model.WorkoutSession;

public class DataActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static boolean DBG=true;
    //private final boolean DBG = true;//Zum Debuggen
    private static final String TAG = "DataActivity"; //Zum Debuggen, gibt Classe an
    private GoogleMap mMap;
    private GraphView mSpeedGraph;

    private GraphView mSpeedAltitudeGraph;

    private LineGraphSeries<DataPoint> mAltitudeSeries;

    private LineGraphSeries<DataPoint> mSpeedSeries;

    private TextView mTextviewStartTime;

    private TextView mTextviewDate;

    private TextView mTextviewDuration;

    private TextView mTextviewDistance;

    private TextView mTextviewAverageSpeed;

    private TextView mTextviewMaxSpeed;

    private WorkoutSession mSession;

    private double mElapsedTime;

    private static String timeUnit;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String MNAME = "onCreate()";
        if (DBG) Log.i(TAG, MNAME + "entering...");


        mSession = (WorkoutSession) getIntent().getParcelableExtra("session");

        initUI();

        initTextViews();


    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);


        mSession = (WorkoutSession) getIntent().getParcelableExtra("session");


        //Zooming and all types of gestures are not allowed
        mMap.getUiSettings().setAllGesturesEnabled(false);

        initMap();

    }
    public void initMap() {
        //Gets all WorkoutLocations from the WorkoutSession
        List<WorkoutLocation> locations = mSession.getLocations();

        PolylineOptions pOptions = new PolylineOptions();
        pOptions.color(Color.RED);
        pOptions.width(5);

        LatLng startPosition = null;
        LatLng endPosition = null;
        LatLng latlng = null;

        int i = 0;

        //Loops through all WorkoutLocations, sets startPosition and endPosition and adds
        //all locations to the PolylineOptions
        for (WorkoutLocation location : locations) {
            latlng = new LatLng(Double.parseDouble(location.getLatitude()),
                    Double.parseDouble(location.getLongitude()));
            if (i == 0)
                startPosition = latlng;
            else if (i == locations.size() - 1)
                endPosition = latlng;

            pOptions.add(latlng);
            i++;
        }
    }

    private void initUI() {
        setContentView(R.layout.activity_data);

        mTextviewStartTime = (TextView) findViewById(R.id.textviewStartTime);
        mTextviewDate = (TextView) findViewById(R.id.textviewDate);
        mTextviewDuration = (TextView) findViewById(R.id.textviewDuration);
        mTextviewDistance = (TextView) findViewById(R.id.textviewDistance);
        mTextviewAverageSpeed = (TextView) findViewById(R.id.textviewAverageSpeed);
        mTextviewMaxSpeed = (TextView) findViewById(R.id.textviewMaxSpeed);

    }


    public void initTextViews() {
        final String MNAME = "initTextViews()";
        if (DBG) Log.i(TAG, MNAME + "entering...");


        //Sets the start date
        String startDate = mSession.getStartDate();
        mTextviewDate.setText("Start date: " + startDate);

        //Sets the start time
        String startTime = mSession.getStartTime();
        mTextviewStartTime.setText("Start time: " + startTime);

        //Sets and formats the duration
        mTextviewDuration.setText("Duration: " + DateUtils.formatElapsedTime(mSession.getDuration()));

        //Sets the distance in km, formatted with 1 decimal
        float distance = HelperUtils.formatFloat(mSession.getDistance() / 1000);
        mTextviewDistance.setText("Distance: " + Float.toString(distance) + " km");

        //Sets the average speed
        double averageSpeed = getIntent().getDoubleExtra("averageSpeed", 0.0);
        mTextviewAverageSpeed.setText("Avg. speed: " + averageSpeed + " km/h");

        //Sets the max speed
        String maxSpeed = getIntent().getStringExtra("maxSpeed");
        mTextviewMaxSpeed.setText("Max speed: " + maxSpeed + " km/h");

        if (DBG) Log.i(TAG, MNAME + "exeting...");
    }
}
