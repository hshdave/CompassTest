package com.zoom.event.compasstest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {
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
    private float[] magnitude_values = null;
    private float[] accelerometer_values = null;
    private boolean sensorReady = false;
    private long sensorCallbacksCount = 0;
    private Bitmap arrowImg;
    private AsyncTask<Void, Void, Void> asyncTask;

    // UI Components
    private ImageView arrow, imgV90;
    private TextView txt_deg, random_text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions();

        initUi();

        initData();
    }

    private void requestPermissions() {
        String[] neededPermissions = {
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
        ActivityCompat.requestPermissions(this, neededPermissions, CODE_PERMISSIONS);
    }

    private void initUi() {
        //views setups
        arrow = findViewById(R.id.img_arrow);
        //imgV90 = findViewById(R.id.img_arrow2);
        txt_deg = findViewById(R.id.txt_degree);
        random_text = findViewById(R.id.random_text);
    }

    private void initData() {
        // Decode the drawable into a bitmap
        arrowImg = BitmapFactory.decodeResource(getResources(), R.drawable.arrow3);

        //magnetic sensors setups
        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //location manager setups
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        destinationObj = new Location(locationManager.NETWORK_PROVIDER);
        destinationObj.setLatitude(45.49519858555764);
        destinationObj.setLongitude(-73.58037471775334);

        //check for user permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            txt_deg.setText("Please enable location service");
            return;
        }


        //indoor atlas location manager setups
        mIALocationManager = IALocationManager.create(this);

        // location getting updated over here
        random_text.setText("Done!");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIALocationManager.requestLocationUpdates(IALocationRequest.create(), mIALocationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
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
        LocationObj = location;
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
                }

                float azimuth = sensorEvent.values[0];
                if (magnitude_values != null && accelerometer_values != null && sensorReady) {
                    sensorReady = false;

                    float[] R = new float[16];
                    float[] I = new float[16];

                    SensorManager.getRotationMatrix(R, I, accelerometer_values, magnitude_values);

                    azimuth = (int) (Math.toDegrees(SensorManager.getOrientation(R, actual_orientation)[0]) + 360) % 360;

                    Log.v(TAG, actual_orientation + "" + (char) 0x00B0);
                }

                float baseAzimuth = azimuth;

                GeomagneticField geoField = new GeomagneticField(Double
                        .valueOf(LocationObj.getLatitude()).floatValue(), Double
                        .valueOf(LocationObj.getLongitude()).floatValue(),
                        Double.valueOf(LocationObj.getAltitude()).floatValue(),
                        System.currentTimeMillis());

                azimuth -= geoField.getDeclination(); // converts magnetic north into true north

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
        }
    }

    private IALocationListener mIALocationListener = new IALocationListenerSupport() {

        /**
         * Location changed, move marker and camera position.
         */


        @Override
        public void onLocationChanged(IALocation location) {
            random_text.setText("");
            random_text.setText("Latitude :: " + location.getLatitude() + " Longitude :: " + location.getLongitude() + " altitude :: " + location.getAltitude());
            LocationObj = location.toLocation();
            /*pointerIcon = findViewById(R.id.icon);
            Log.d("Log", "new location received with coordinates: " + location.getLatitude()
                    + "," + location.getLongitude());


            txt_my.setText("Latitude : " + location.getLatitude());
            txt_atlas.setText(" Longitude : " + location.getLongitude());


            txt_dis.setText("Distance : "+myObjDis(45.49518337252893,45.49518337252893,location.toLocation()));


            random_text.setText("");
            random_text.setText("on location changed!   Lati :: "+location.getLatitude() +" Longi :: "+location.getLongitude());
            try{
                lati = location.getLatitude();
                longi = location.getLongitude();



                Double delta_x = lati - 45.49518337252893;
                Double delta_y = (longi)-(-73.58034846752349);
                Double theta_radians = Math.atan2(delta_y, delta_x);
                Double newDegree = Math.toDegrees(theta_radians);


                //PointF origin = new PointF();

                RotateAnimation rs= new RotateAnimation(currentDegree, newDegree.floatValue(), Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,
                        0.5F);
                rs.setDuration(1000);
                rs.setFillAfter(true);

                arrow.startAnimation(rs);

                currentDegree = -newDegree.floatValue();

                txt_deg.setText("");
                txt_deg.setText("Degree -> " + newDegree .toString() + " Radians--> " +theta_radians.toString() + (char) 0x00B0);
            }catch(Exception ex){
                Log.d("Exception :: ", "onLocationChanged: "+ex);
            }*/
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

        /*
        int degree = Math.round(sensorEvent.values[0]);

        txt_deg.setText(Integer.toString(degree) + (char) 0x00B0);

//        Double delta_x = lati - 45.4951929;
//        Double delta_y = (longi)-(-73.5803635);
//        Double theta_radians = Math.atan2(delta_y, delta_x);
//        Double newDegree = Math.toDegrees(theta_radians);


        PointF origin = new PointF();

        RotateAnimation rs= new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,
                        0.5F);
        //RotateAnimation rs2= new RotateAnimation(currentDegree, -degree+90, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,
               // 0.5F);


        rs.setDuration(1000);
        rs.setFillAfter(true);
        //rs2.setDuration(1000);
        //rs2.setFillAfter(true);


        arrow.startAnimation(rs);
        //imgV90.startAnimation(rs2);


        currentDegree = -degree;
        txt_deg.setText(sensorEvent.sensor.getType() + (char) 0x00B0);*/