package appentwicklung.android.mapsapi_run;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {
    private final boolean DBG = true;//Zum Debuggen
    private static final String TAG = "MainActivity"; //Zum Debuggen, gibt Classe an

    private static final int ACCESS_COARSE_LOCATION = 100;
    private static final int ACCESS_FINE_LOCATION = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION);
          }



    // Function überprüft ob Permissions gegeben sind, wenn gegeben dann Toast
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(MainActivity.this, permission) == PackageManager.PERMISSION_DENIED) {

            // Requesting the permission
            ActivityCompat.requestPermissions(MainActivity.this, new String[] { permission }, requestCode);
        }
        else {
            Toast.makeText(MainActivity.this, "Permission already granted", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     *Funktion: Wenn Permission nicht gegeben wird wird die Zustimmung des Nutzer erfragt
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == ACCESS_COARSE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Location Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Location Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
    //on Click Listener Funktion: führt zur Hauptfunktionalität der App
    public void onClickOpenMaps(View view){

        Intent intent =new Intent();
        intent.setClass(getApplicationContext(), MapsActivity.class);
        setContentView(R.layout.activity_maps);
        startActivity(intent);
    }
    //on Click Listener Funktion: Führt zu den Statistiken der aufgezeichneten WorkoutSessions
    public void onClickToStatictic (View view){

        Intent intent =new Intent();

        intent.setClass(getApplicationContext(), DataActivity.class);

        setContentView(R.layout.activity_data);

        startActivity(intent);
    }
    //on Click Listener Funktion: zum Schließen der App
    public void onClickCloseApp(View view){
        finish();
        System.exit(0);
    }
}