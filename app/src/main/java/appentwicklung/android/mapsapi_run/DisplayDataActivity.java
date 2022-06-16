package appentwicklung.android.mapsapi_run;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;

import appentwicklung.android.mapsapi_run.helper.DBHelper;
import appentwicklung.android.mapsapi_run.helper.HelperUtils;
import appentwicklung.android.mapsapi_run.model.WorkoutLocation;
import appentwicklung.android.mapsapi_run.model.WorkoutSession;


public class DisplayDataActivity extends AppCompatActivity implements OnMapReadyCallback {
////___________View Workout Session Activity_________
    /**
     * List of WorkoutSessions
     */
    private List<WorkoutSession> mSessions;

    /**
     * Id of WorkoutSession
     */
    private long mSessionId;

    /**
     * DBHelper object
     */
    private DBHelper myDatabase;
////___________ViewSessionStatisticsActivity_________
    /**
     * Speed graph
     */
    private GraphView mSpeedGraph;

    /**
     * Altitude graph
     */
    private GraphView mAltitudeGraph;

    /**
     * Speed&Altitude graph
     */
    private GraphView mSpeedAltitudeGraph;


    /**
     * Speed series
     */
    private LineGraphSeries<DataPoint> mSpeedSeries;

    /**
     * Start time TextView
     */
    private TextView mTextviewStartTime;

    /**
     * Start date TextView
     */
    private TextView mTextviewDate;

    /**
     * Duration TextView
     */
    private TextView mTextviewDuration;

    /**
     * Distance TextView
     */
    private TextView mTextviewDistance;

    /**
     * Average speed TextView
     */
    private TextView mTextviewAverageSpeed;

    /**
     * Max speed TextView
     */
    private TextView mTextviewMaxSpeed;

    /**
     * WorkoutSession
     */
    private WorkoutSession mSession;

    /**
     * Elapsed time
     */
    private double mElapsedTime;

    /**
     * Time unit, for the graphs
     */
    private static String timeUnit;

    //_________________View SessionDetail Activity_____________

    /**
     * GoogleMap
     */
    private GoogleMap mMap;


    ////___________View Workout Session Activity_________
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        mSession = (WorkoutSession) getIntent().getParcelableExtra("session");

        initUI();

        setTimeUnit(mSession.getDuration());

        initSpeedGraph();

        initTextViews();


        //Constructs the data source
        myDatabase = new DBHelper(this.getApplicationContext());
        mSessions = myDatabase.getAllWorkoutSessions();

    }


    ////___________ViewSessionStatisticsActivity_________

    /**
     * Inits several UI components
     */
    public void initUI() {
        setContentView(R.layout.activity_data);

        //Enables the Up button

        mSpeedGraph = (GraphView) findViewById(R.id.speedGraph);
        mTextviewStartTime = (TextView) findViewById(R.id.textviewStartTime);
        mTextviewDate = (TextView) findViewById(R.id.textviewDate);
        mTextviewDuration = (TextView) findViewById(R.id.textviewDuration);
        mTextviewDistance = (TextView) findViewById(R.id.textviewDistance);
        mTextviewAverageSpeed = (TextView) findViewById(R.id.textviewAverageSpeed);
        mTextviewMaxSpeed = (TextView) findViewById(R.id.textviewMaxSpeed);
    }

    /**
     * Converts the elapsed time to the right time unit
     *
     * @param elapsedTime elapsed time
     * @return converted elapsed time (in the right time unit)
     */
    public double convertElapsedTime(long elapsedTime) {
        double time = 0;

        if (timeUnit.equals("minutes"))
            time = (double) elapsedTime / 60;
        else if (timeUnit.equals("hours"))
            time = (double) elapsedTime / (60 * 60);

        return time;
    }


    /**
     * Inits the Speed graph
     */
    public void initSpeedGraph() {
        DataPoint[] dp = new DataPoint[mSession.getLocations().size()];

        int i = 0;

        //Adds datapoints (speeds and time) from all WorkoutLocations in the WorkoutSession
        for (WorkoutLocation location : mSession.getLocations()) {

            //If timeUnit is minutes or hours, convert the elapsedTime to the right time unit,
            //then create the DataPoint
            if (timeUnit.equals("minutes") || timeUnit.equals("hours")) {
                mElapsedTime = convertElapsedTime(location.getElapsedTime());
                dp[i] = new DataPoint(mElapsedTime, location.getSpeed());
            }
            //If timeUnit is seconds, just create the DataPoint
            else {
                mElapsedTime = location.getElapsedTime();
                dp[i] = new DataPoint(mElapsedTime, location.getSpeed());
            }
            i++;
        }

        //Creates the speed series from the DataPoints
        mSpeedSeries = new LineGraphSeries<>(dp);

        //Sets up the speed series
        mSpeedSeries.setAnimated(true);
        mSpeedSeries.setColor(Color.argb(255, 255, 60, 60));

        //Sets the x axis bounds of the graph manually
        mSpeedGraph.getViewport().setXAxisBoundsManual(true);
        mSpeedGraph.getViewport().setMaxX(mElapsedTime);

        //Sets up the speed graph
        mSpeedGraph.setTitle("Speed");
        mSpeedGraph.setTitleTextSize(50);
        mSpeedGraph.getGridLabelRenderer().setHorizontalAxisTitle(timeUnit);
        mSpeedGraph.getGridLabelRenderer().setVerticalAxisTitle("km/h");
        mSpeedGraph.getGridLabelRenderer().setLabelVerticalWidth(30);
        mSpeedGraph.getGridLabelRenderer().setPadding(45);
        mSpeedGraph.getGridLabelRenderer().setVerticalAxisTitleTextSize(30);
        mSpeedGraph.getGridLabelRenderer().setHorizontalAxisTitleTextSize(30);

        //Adds the speed series to the speed graph
        mSpeedGraph.addSeries(mSpeedSeries);
    }


    /**
     * Calculates the time unit of the graphs (seconds/minutes/hours)
     *
     * @param duration duration of the WorkoutSession
     */
    public static void setTimeUnit(long duration) {
        timeUnit = "";
        if (duration < 60)
            timeUnit = "seconds";
        else if (duration > 60 && duration < 60 * 60)
            timeUnit = "minutes";
        else if (duration >= 60 * 60)
            timeUnit = "hours";
    }


    //_________________View SessionDetail Activity__________

    /**
     * When the GoogleMap is ready - sets options and calls initMap()
     *
     * @param googleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);

        //Zooming and all types of gestures are not allowed
        mMap.getUiSettings().setAllGesturesEnabled(false);

        initMap();
    }

    /**
     * Draws a polyline between all WorkoutLocations in the WorkoutSession. Also adds start
     * and end markers and bounds for the GoogleMap.
     */
    public void initMap() {
        //Gets all WorkoutLocations from the WorkoutSession
        List<WorkoutLocation> locations = mSession.getLocations();

        PolylineOptions pOptions = new PolylineOptions();
        pOptions.color(Color.YELLOW);
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

        //Adds Markers for the startPosition and endPosition and the polyline
        mMap.addMarker(new MarkerOptions()
                .position(startPosition)
                .title("START"));
        mMap.addPolyline(pOptions);
        mMap.addMarker(new MarkerOptions()
                .position(endPosition)
                .title("END"));

        List<LatLng> pointsList = pOptions.getPoints();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

        //Sets bounds (includes all locations)
        for (LatLng point : pointsList) {
            bounds.include(point);
        }

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds.build(), 400, 400, 0);
        mMap.moveCamera(cu);                    //Moves camera to include all bounds
    }

    /**
     * Starts the ViewSessionStatistics activity
     */
    public void viewStatistics(View v) {
        Intent intent = new Intent(this, DataActivity.class);
        intent.putExtra("session", mSession);
        intent.putExtra("averageSpeed", HelperUtils.calculateAverageSpeed(mSession));
        intent.putExtra("maxSpeed", Float.toString(HelperUtils.getMaxSpeed(mSession)));

        intent.putExtra("parentActivity", new String("main"));

        startActivity(intent);
    }

    /**
     * Inits the textviews
     */
    public void initTextViews() {

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
    }


    public void onClickBack(View view) {

        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

    }

}




