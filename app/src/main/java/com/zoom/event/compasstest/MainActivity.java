package com.zoom.event.compasstest;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

import java.io.IOException;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SurfaceHolder.Callback,LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static final int CODE_PERMISSIONS = 0;
    private static SensorManager sensorService;

    private Sensor sensor;
    private IALocationManager mIALocationManager;
    private LocationManager locationManager;
    private Location LocationObj, destinationObj;
    private float currentDegree = 0f;
    private double lati = 0, longi = 0;
    private static float[] magnitude_values = null;
    private static float[] accelerometer_values = null;
    private static boolean sensorReady = false;
    private long sensorCallbacksCount = 0;
    private Bitmap arrowImg;
    private AsyncTask<Void, Void, Void> asyncTask;

    // UI Components
    private ImageView arrow, imgV90;
    private TextView txt_deg, random_text;

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraviewOn = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("Called","OnCreate Called");

       // requestPermissions();

        initUi();

        initData();

        //indoor atlas location manager setups
        mIALocationManager = IALocationManager.create(this);

    }

    private void requestPermissions() {
        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
        };
        ActivityCompat.requestPermissions(this, neededPermissions, CODE_PERMISSIONS);
    }

    private void initUi() {
        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView surfaceView =  findViewById(R.id.cameraview);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //views setups
        arrow = findViewById(R.id.img_arrow);
        //imgV90 = findViewById(R.id.img_arrow2);
        txt_deg = findViewById(R.id.txt_degree);
        random_text = findViewById(R.id.random_text);

        // location getting updated over here
        random_text.setText("Done!");

    }



    private void initData() {
        // Decode the drawable into a bitmap
        arrowImg = BitmapFactory.decodeResource(getResources(), R.drawable.arrow3);

        //magnetic sensors setups
        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //location manager setups
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //destinationObj = new Location(locationManager.NETWORK_PROVIDER);
        destinationObj = new Location("");
        destinationObj.setLatitude(45.49515126854131);
        destinationObj.setLongitude(-73.58030833303928);


        //check for user permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            txt_deg.setText("Please enable location service");

            String[] neededPermissions = {
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
            };
            ActivityCompat.requestPermissions(this, neededPermissions, CODE_PERMISSIONS);


        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        return;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("Called","OnResume Called");
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.v("Called","OnStart Called");
        if (sensor != null) {
            sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorService.registerListener(this, sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Not Supported!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.v("Called","OnStop Called");
        sensorService.unregisterListener(this);
    }

    /**
     * Following links
     * https://stackoverflow.com/questions/7978618/rotating-an-imageview-like-a-compass-with-the-north-pole-set-elsewhere
     * https://stackoverflow.com/questions/10160144/android-destination-location
     *
     * @param sensorEvent
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        ++sensorCallbacksCount;

        if (sensorCallbacksCount % 5 != 0 || LocationObj == null || asyncTask != null) {
            return;
        }

        asyncTask = new AsyncSensorUpdater(sensorEvent);
        asyncTask.execute();
    }

    /**
     * rotate the bitmap image.
     * Always run this method on main thread
     */
    private synchronized void rotateImageView(ImageView imageView, Bitmap arrowImg, float rotate) {
        // Get the width/height of the drawable
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = arrowImg.getWidth(), height = arrowImg.getHeight();

        // Initialize a new Matrix
        Matrix matrix = new Matrix();

        // Decide on how much to rotate
        rotate = rotate % 360;

        // Actually rotate the image
        matrix.postRotate(rotate, width, height);

        // recreate the new Bitmap via a couple conditions
        Bitmap rotatedBitmap = Bitmap.createBitmap(arrowImg, 0, 0, width, height, matrix, true);
        //BitmapDrawable bmd = new BitmapDrawable( rotatedBitmap );

        imageView.setImageBitmap(rotatedBitmap);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onLocationChanged(Location location) {
        // LocationObj = location;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try
        {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
        }catch (Exception e)
        {
            e.printStackTrace();
        }


    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        if (isCameraviewOn) {
            mCamera.stopPreview();
            isCameraviewOn = false;
        }

        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                isCameraviewOn = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    private class AsyncSensorUpdater extends AsyncTask<Void, Void, Void> {
        private SensorEvent sensorEvent;
        float direction;
        String bearingText;

        private AsyncSensorUpdater(SensorEvent sensorEvent) {
            this.sensorEvent = sensorEvent;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            synchronized (LocationObj) {
                float[] actual_orientation = new float[3];
                switch (sensorEvent.sensor.getType()) {
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        magnitude_values = sensorEvent.values.clone();
                        sensorReady = true;
                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        accelerometer_values = sensorEvent.values.clone();
                        sensorReady = true;
                }


                Log.v("Sensor Values:", magnitude_values + "  " + accelerometer_values + " " + sensorReady);

                float azimuth = 0;
                if (magnitude_values != null && accelerometer_values != null && sensorReady) {


                    float[] R = new float[16];
                    float[] I = new float[16];

                    SensorManager.getRotationMatrix(R, I, accelerometer_values, magnitude_values);

                    azimuth = (int) (Math.toDegrees(SensorManager.getOrientation(R, actual_orientation)[0]) + 360) % 360;

                    Log.v(TAG, azimuth + "" + (char) 0x00B0);
                }

                float baseAzimuth = azimuth;

                GeomagneticField geoField = new GeomagneticField(Double
                        .valueOf(LocationObj.getLatitude()).floatValue(), Double
                        .valueOf(LocationObj.getLongitude()).floatValue(),
                        Double.valueOf(LocationObj.getAltitude()).floatValue(),
                        System.currentTimeMillis());

                azimuth -= geoField.getDeclination(); // converts magnetic north into true north


                Log.v("Accurecy", String.valueOf(destinationObj.getAccuracy()));
                // Store the bearingTo in the bearTo variable
                float bearTo = LocationObj.bearingTo(destinationObj);
                // If the bearTo is smaller than 0, add 360 to get the rotation clockwise.
                if (bearTo < 0) {
                    bearTo = bearTo + 360;
                }

                //This is where we choose to point it
                direction = bearTo - azimuth;
                // If the direction is smaller than 0, add 360 to get the rotation clockwise.
                if (direction < 0) {
                    direction += 360;
                }

                if ((360 >= baseAzimuth && baseAzimuth >= 337.5)
                        || (0 <= baseAzimuth && baseAzimuth <= 22.5)) bearingText = "N";
                else if (baseAzimuth > 22.5 && baseAzimuth < 67.5) bearingText = "NE";
                else if (baseAzimuth >= 67.5 && baseAzimuth <= 112.5) bearingText = "E";
                else if (baseAzimuth > 112.5 && baseAzimuth < 157.5) bearingText = "SE";
                else if (baseAzimuth >= 157.5 && baseAzimuth <= 202.5) bearingText = "S";
                else if (baseAzimuth > 202.5 && baseAzimuth < 247.5) bearingText = "SW";
                else if (baseAzimuth >= 247.5 && baseAzimuth <= 292.5) bearingText = "W";
                else if (baseAzimuth > 292.5 && baseAzimuth < 337.5) bearingText = "NW";
                else bearingText = "?";
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            rotateImageView(arrow, arrowImg, direction);
            txt_deg.setText("" + direction + " - (" + bearingText + ")");
            asyncTask = null;
            sensorReady = false;
        }
    }

    private IALocationListener mIALocationListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */


        @Override
        public void onLocationChanged(IALocation location) {
            random_text.setText("");
            random_text.setText("Latitude :: " + location.getLatitude() + " Longitude :: " + location.getLongitude() + " Accuracy :: " + location.getAccuracy());
            LocationObj = location.toLocation();

            Log.v("IA Location Changed", location.toString());
            }
    };


    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
    }

    @Override
    public void onProviderEnabled(String s) {
    }

    @Override
    public void onProviderDisabled(String s) {
    }
}