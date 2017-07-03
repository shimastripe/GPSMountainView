package com.shimastripe.gpsmountainview;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements LocationListener, SensorEventListener {

    private final static String TAG = "MainActivity";

    private SensorManager sensorMgr;
    private Sensor accelerometer;
    private Sensor magneticField;
    private float[] fAccell = null;
    private float[] fMagnetic = null;
    private float azimuth;
    private float altura;

    private LocationManager locationManager;
    private double latitude;
    private double longtitude;
    private TextView textView1, textView2, textView3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        textView1 = (TextView) findViewById(R.id.text_view1);
        textView2 = (TextView) findViewById(R.id.text_view2);
        textView3 = (TextView) findViewById(R.id.text_view3);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
        } else {
            locationStart();
        }

        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            Toast.makeText(this, getString(R.string.toast_no_accel_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        magneticField = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField == null) {
            Toast.makeText(this, getString(R.string.toast_no_accel_error),
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorMgr.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        sensorMgr.unregisterListener(this);
    }

    public void onClickStartButton(View view) {
        Log.d(TAG, "onCliclStartButton()");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DocomoAPIInterface.END_POINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DocomoAPIInterface service = retrofit.create(DocomoAPIInterface.class);
        service.getMountainData(latitude, longtitude, azimuth, altura, 45, getString(R.string.DOCOMO_API_KEY)).enqueue(new Callback<MountainRepository>() {
            @Override
            public void onResponse(Call<MountainRepository> call, Response<MountainRepository> response) {
                Log.d(TAG, "Succeed to request");
                MountainRepository mr = response.body();
            }

            @Override
            public void onFailure(Call<MountainRepository> call, Throwable t) {
                Log.d(TAG, "Failed to request");
                Log.d(TAG,t.toString());
            }
        });
    }

    private void locationStart() {
        Log.d(TAG, "locationStart()");

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        final boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (!gpsEnabled) {
            Intent settingsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(settingsIntent);
            Log.d(TAG, "not gpsEnable, startActivity");
        } else {
            Log.d(TAG, "gpsEnabled");
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,}, 1000);
            Log.d(TAG, "checkSelfPermission false");
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 50, this);
        Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        latitude = loc.getLatitude();
        longtitude = loc.getLongitude();
        setLocation(latitude, longtitude);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1000) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "checkSelfPermission true");
                locationStart();
                return;

            } else {
                Toast toast = Toast.makeText(this, "これ以上なにもできません", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                Log.d(TAG, "LocationProvider.AVAILABLE");
                break;
            case LocationProvider.OUT_OF_SERVICE:
                Log.d(TAG, "LocationProvider.OUT_OF_SERVICE");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                Log.d(TAG, "LocationProvider.TEMPORARILY_UNAVAILABLE");
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        latitude = location.getLatitude();
        longtitude = location.getLongitude();
        setLocation(latitude, longtitude);
    }

    private void setLocation(double latitude, double longtitude) {
        textView1.setText("Latitude:" + latitude);
        textView2.setText("Longitude:" + longtitude);
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Called when the provider is enabled by the user.
        Log.d(TAG, "onProviderEnabled()");
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Called when the provider is disabled by the user.
        Log.d(TAG, "onProviderDisabled");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.i(TAG, "onAccuracyChanged: ");
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                fAccell = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                fMagnetic = event.values.clone();
                break;
        }

        if (fAccell != null && fMagnetic != null) {
            // 回転行列を得る
            float[] inR = new float[9];
            SensorManager.getRotationMatrix(
                    inR,
                    null,
                    fAccell,
                    fMagnetic);
            // ワールド座標とデバイス座標のマッピングを変換する
            float[] outR = new float[9];
            SensorManager.remapCoordinateSystem(
                    inR,
                    SensorManager.AXIS_X, SensorManager.AXIS_Y,
                    outR);
            // 姿勢を得る
            float[] fAttitude = new float[3];
            SensorManager.getOrientation(
                    outR,
                    fAttitude);

            azimuth = rad2deg(fAttitude[0]);
            altura = rad2deg(fAttitude[1]);

            String buf =
                    "---------- Orientation --------\n" +
                            String.format("方位角\n\t%f\n", azimuth) +
                            String.format("前後の傾斜\n\t%f\n", altura) +
                            String.format("左右の傾斜\n\t%f\n", rad2deg(fAttitude[2]));
            textView3.setText(buf);
        }
    }

    private float rad2deg(float rad) {
        return rad * (float) 180.0 / (float) Math.PI;
    }
}
