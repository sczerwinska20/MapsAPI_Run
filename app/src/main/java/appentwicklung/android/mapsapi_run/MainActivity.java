package appentwicklung.android.mapsapi_run;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



// Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        ((DatabaseReference) myRef).setValue("Hello, World!");
    }

    public void onClickOpenMaps(View view){
        Intent intent =new Intent();
        intent.setClass(getApplicationContext(),MapsActivity.class);
        startActivity(intent);
    }
}