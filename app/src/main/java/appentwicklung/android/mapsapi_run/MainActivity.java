package appentwicklung.android.mapsapi_run;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


       // FirebaseDatabase.getInstance().getReference().setValue("This is my apptracker");

    }

    public void onClickOpenMaps(View view){
        Intent intent =new Intent();
        intent.setClass(getApplicationContext(),MapsActivity.class);
        startActivity(intent);
    }
}