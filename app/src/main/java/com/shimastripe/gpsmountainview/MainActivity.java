package com.shimastripe.gpsmountainview;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        SensorEventListener {

    private final static String TAG = "MainActivity";

    // Sensor
    private SensorManager sensorMgr;
    private Sensor accelerometer;
    private Sensor magneticField;
    private float[] fAccell = null;
    private float[] fMagnetic = null;
    private float azimuth;
    private float alturaV;
    private float alturaH;

    // GPS
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location lastLocation;

    private enum UpdatingState {STOPPED, REQUESTING, STARTED}

    private UpdatingState state = UpdatingState.STOPPED;

    private final static String[] PERMISSIONS = {
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private final static int REQCODE_PERMISSIONS = 1111;

    // View
    private TextView textView1, textView2, textView3;
    private LineChart lineChart;
    private List<Integer> ridges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        textView1 = (TextView) findViewById(R.id.text_view1);
        textView2 = (TextView) findViewById(R.id.text_view2);
        textView3 = (TextView) findViewById(R.id.text_view3);

        buildGoogleApiClient();
        createLocationRequest();

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
        lineChart = (LineChart) findViewById(R.id.chart1);

        if (savedInstanceState != null) {
            ridges = savedInstanceState.getIntegerArrayList("RIDGES");
            DrawGraph();
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private void createLocationRequest() {
        Log.d(TAG, "createLocationRequest");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        Log.i(TAG, "Connect to Google Api");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorMgr.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);

        if (state != UpdatingState.STARTED && mGoogleApiClient.isConnected())
            startLocationUpdate(true);
        else
            state = UpdatingState.REQUESTING;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        sensorMgr.unregisterListener(this);
        if (state == UpdatingState.STARTED)
            stopLocationUpdate();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
        if (state == UpdatingState.REQUESTING)
            startLocationUpdate(true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended i" + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed " + connectionResult.getErrorMessage());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        lastLocation = location;
        displayLocation(location);
    }

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult");
        switch (reqCode) {
            case REQCODE_PERMISSIONS:
                startLocationUpdate(false);
                break;
        }
    }

    private void startLocationUpdate(boolean reqPermission) {
        Log.d(TAG, "startLocationUpdate: " + reqPermission);
        for (String permission : PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                if (reqPermission)
                    ActivityCompat.requestPermissions(this, PERMISSIONS, REQCODE_PERMISSIONS);
                else
                    Toast.makeText(this, getString(R.string.toast_requires_permission, permission),
                            Toast.LENGTH_SHORT).show();
                return;
            }
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        state = UpdatingState.STARTED;
    }

    private void stopLocationUpdate() {
        Log.d(TAG, "stopLocationUpdate");
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        state = UpdatingState.STOPPED;
    }

    private void displayLocation(Location loc) {
        Log.d(TAG, "displayLocation");
        textView1.setText("Latitude:" + loc.getLatitude());
        textView2.setText("Longitude:" + loc.getLongitude());
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
            alturaV = rad2deg(fAttitude[1]);
            alturaH = rad2deg(fAttitude[2]);

            String buf =
                    "---------- Orientation --------\n" +
                            String.format("方位角\n\t%f\n", azimuth) +
                            String.format("前後の傾斜(縦向き)\n\t%f\n", alturaV) +
                            String.format("左右の傾斜(縦向き)\n\t%f\n", alturaH);
            textView3.setText(buf);
        }
    }

    private float rad2deg(float rad) {
        return rad * (float) 180.0 / (float) Math.PI;
    }

    public void onClickStartButton(View view) {
        Log.d(TAG, "onClickStartButton()");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(DocomoAPIInterface.END_POINT)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        DocomoAPIInterface service = retrofit.create(DocomoAPIInterface.class);
        service.getMountainData(lastLocation.getLatitude(), lastLocation.getLongitude(), azimuth, alturaH, 45, getString(R.string.DOCOMO_API_KEY)).enqueue(new Callback<MountainRepository>() {
            @Override
            public void onResponse(Call<MountainRepository> call, Response<MountainRepository> response) {
                Log.d(TAG, "Succeed to request");
                MountainRepository mr = response.body();

                if (mr != null) {
                    ridges = mr.getRidge();
                    DrawGraph();
                }
            }

            @Override
            public void onFailure(Call<MountainRepository> call, Throwable t) {
                Log.d(TAG, "Failed to request");
                Log.d(TAG, t.toString());
            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putIntegerArrayList("RIDGES", (ArrayList<Integer>) ridges);
    }

    private void DrawGraph() {
        ArrayList<Entry> entries = new ArrayList<Entry>();

        for (int i = 0; i < ridges.size(); i++) {
            entries.add(new Entry(i, ridges.get(i)));
        }

        //データをセット
        LineDataSet dataSet = new LineDataSet(entries, "稜線");
        //LineDataインスタンス生成
        LineData data = new LineData(dataSet);
        //LineDataをLineChartにセット
        lineChart.setData(data);

        //背景色
        lineChart.setBackgroundColor(Color.WHITE);

        //アニメーション
        lineChart.animateX(1200);
    }
}
