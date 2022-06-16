package appentwicklung.android.mapsapi_run;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void onClickOpenMaps(View view){

        Intent intent =new Intent();
        intent.setClass(getApplicationContext(), MapsActivity.class);
        setContentView(R.layout.activity_maps);
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