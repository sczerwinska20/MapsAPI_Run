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



// Zum testen der Datenbank verbdinung
      /*  FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");

        ((DatabaseReference) myRef).setValue("Hello, World!");*/

        FirebaseDatabase.getInstance().getReference().child("101");
    }

    public void onClickOpenMaps(View view){
        Intent intent =new Intent();
        intent.setClass(getApplicationContext(), MapsActivity.class);
        startActivity(intent);
    }

    public void onClickCloseApp(View view){
        finish();
        System.exit(0);
    }

    public void onClickToStatictic (View view){
        setContentView(R.layout.activity_data);
    }
}