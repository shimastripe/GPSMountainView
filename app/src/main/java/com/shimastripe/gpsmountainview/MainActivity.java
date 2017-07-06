package com.shimastripe.gpsmountainview;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.nfc.Tag;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener,
        ResultCallback<LocationSettingsResult>, SensorEventListener {

    private final static String TAG = "MainActivity";

    private SensorManager sensorMgr;
    private Sensor accelerometer;
    private Sensor magneticField;
    private float[] fAccell = null;
    private float[] fMagnetic = null;
    private float azimuth;
    private float alturaV;
    private float alturaH;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;
    protected LocationSettingsRequest mLocationSettingsRequest;
    private final static int REQCODE_PERMISSIONS = 0x1;

    private TextView textView1, textView2, textView3;

    private LineChart lineChart;

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
        buildLocationSettingsRequest();

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
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void buildLocationSettingsRequest() {
        Log.d(TAG, "buildLocationSettingsRequest");
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
        mGoogleApiClient.connect();
        Log.i(TAG, "Connect to Google Api");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        sensorMgr.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorMgr.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_FASTEST);

        if (isLocationEnabled(this.getApplicationContext())) {
            if (mGoogleApiClient.isConnected()) {
                startLocationUpdates();
            }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "onPause");
        sensorMgr.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        Log.d(TAG, "onConnected");
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                updateLocationUI();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended i" + i);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged");
        setLocation();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed " + connectionResult.getErrorMessage());
    }

    @Override
    public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
        final Status status = locationSettingsResult.getStatus();
        switch (status.getStatusCode()) {
            case LocationSettingsStatusCodes.SUCCESS:
                Log.i(TAG, "All location settings are satisfied.");
                startLocationUpdates();
                break;
            case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                Log.i(TAG, "Location settings are not satisfied. Show the user a dialog to" +
                        "upgrade location settings ");

                try {
                    // Show the dialog by calling startResolutionForResult(), and check the result
                    // in onActivityResult().
                    status.startResolutionForResult(this, REQCODE_PERMISSIONS);
                } catch (IntentSender.SendIntentException e) {
                    Log.i(TAG, "PendingIntent unable to execute request.");
                }
                break;
            case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                Log.i(TAG, "Location settings are inadequate, and cannot be fixed here. Dialog " +
                        "not created.");
                break;
        }
    }

    public static boolean isLocationEnabled(Context context) {
        Log.d(TAG, "isLocationEnabled");
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    private void updateLocationUI() {
        Log.d(TAG, "updateLocationUI");
        if (mCurrentLocation != null) {
            setLocation();
        } else {
            startLocationUpdates();
        }
    }

    private void setLocation() {
        Log.d(TAG, "setLocation");
        if (mCurrentLocation != null) {
            textView1.setText("Latitude:" + mCurrentLocation.getLatitude());
            textView2.setText("Longitude:" + mCurrentLocation.getLongitude());
        }
    }

    protected void startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates");
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            } else {
            }
            return;
        } else {
        }

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(Status status) {
                if (status.isSuccess()) {
                    Log.d(TAG, "App Indexing API: Recorded recipe view end successfully." + status.toString());
                } else {
                    Log.d(TAG, "App Indexing API: There was an error recording the recipe view."
                            + status.toString());
                }
            }
        });

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
        service.getMountainData(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude(), azimuth, alturaH, 45, getString(R.string.DOCOMO_API_KEY)).enqueue(new Callback<MountainRepository>() {
            @Override
            public void onResponse(Call<MountainRepository> call, Response<MountainRepository> response) {
                Log.d(TAG, "Succeed to request");
                MountainRepository mr = response.body();
                if (mr != null) {
                    ArrayList<Entry> entries = new ArrayList<Entry>();
                    List<Integer> list = mr.getRidge();
                    for (int i = 0; i < list.size(); i++) {
                        entries.add(new Entry(i, list.get(i)));
                    }
                    //データをセット
                    LineDataSet dataSet = new LineDataSet(entries, "weight");
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

            @Override
            public void onFailure(Call<MountainRepository> call, Throwable t) {
                Log.d(TAG, "Failed to request");
                Log.d(TAG, t.toString());
            }
        });
    }
}
