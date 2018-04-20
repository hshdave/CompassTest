package com.zoom.event.compasstest;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.indooratlas.android.sdk.IALocation;
import com.indooratlas.android.sdk.IALocationListener;
import com.indooratlas.android.sdk.IALocationManager;
import com.indooratlas.android.sdk.IALocationRequest;
import com.indooratlas.android.sdk.resources.IALocationListenerSupport;

import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SensorEventListener, SurfaceHolder.Callback,LocationListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 1;
    private static final int CODE_PERMISSIONS = 0;
    private static final float ALPHA = 0.8f;
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

    private ArrayList<Locationdata> locarray;

    int count=0;

    // UI Components
    private ImageView arrow, finaimg;
    /*private TextView txt_deg, random_text,txt_cnt;*/
    private TextView txt_dis;

    private Camera mCamera;
    private SurfaceHolder mSurfaceHolder;
    private boolean isCameraviewOn = false;


    private float grav[] = new float[3];
    private float mag[] = new float[3];
    protected float[] gravSensorVals;
    protected float[] magSensorVals;

    private float RTmp[] = new float[9];
    private float Rot[] = new float[9];
    private float I[] = new float[9];
    private float results[] = new float[3];

    TextView txt_lati, txt_longi;
    static int cnt = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.v("Called","OnCreate Called");

        locarray = new ArrayList<>();

        /*locarray.add(new Locationdata(45.49544786095555,-73.57820212841035));
        locarray.add(new Locationdata(45.49549204416089,-73.57815787196161));
        locarray.add(new Locationdata(45.49543093971879,-73.57803180813791));
        locarray.add(new Locationdata(45.49551930612137,-73.57794463634492));
        locarray.add(new Locationdata(45.495451621229705,-73.57781052589418));*/


        locarray.add(new Locationdata(45.49552626261378, -73.57822626829149));
        locarray.add(new Locationdata(45.495505581130274, -73.57818335294725));
        locarray.add(new Locationdata(45.49559676761408, -73.57809618115425));
        locarray.add(new Locationdata(45.49572461645614, -73.57836440205575));

        initUi();

        initData();

        //indoor atlas location manager setups
        mIALocationManager = IALocationManager.create(this);

    }
    private void initUi() {
        getWindow().setFormat(PixelFormat.UNKNOWN);
        SurfaceView surfaceView =  findViewById(R.id.cameraview);
        mSurfaceHolder = surfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        //views setups
        arrow = findViewById(R.id.img_arrow);
        finaimg = findViewById(R.id.dialog_img);

        finaimg.setVisibility(View.GONE);

        /*txt_deg = findViewById(R.id.txt_degree);
        random_text = findViewById(R.id.random_text);
        txt_cnt = findViewById(R.id.count);

        txt_lati = findViewById(R.id.current_lati);
        txt_longi =  findViewById(R.id.current_longi);*/

        txt_dis = findViewById(R.id.txt_distance);

        // location getting updated over here
        //random_text.setText("Done!");

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
        //destinationObj.setLatitude(45.49542952961549);
        //destinationObj.setLongitude(-73.57802778482439);


        //check for user permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //txt_deg.setText("Please enable location service");

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
     * @param evt
     */
    @Override
    public void onSensorChanged(SensorEvent evt) {
        ++sensorCallbacksCount;

        if (sensorCallbacksCount % 5 != 0 || LocationObj == null || asyncTask != null) {
            return;
        }

        asyncTask = new AsyncSensorUpdater(evt);
        asyncTask.execute();


    }

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
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

                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    accelerometer_values = lowPass(sensorEvent.values.clone(), gravSensorVals);
                    grav[0] = sensorEvent.values[0];
                    grav[1] = sensorEvent.values[1];
                    grav[2] = sensorEvent.values[2];
                    sensorReady = true;

                } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    magnitude_values = lowPass(sensorEvent.values.clone(), magSensorVals);
                    mag[0] = sensorEvent.values[0];
                    mag[1] = sensorEvent.values[1];
                    mag[2] = sensorEvent.values[2];
                    mag[2] = sensorEvent.values[2];
                    sensorReady = true;

                }


               /* switch (sensorEvent.sensor.getType()) {
                    case Sensor.TYPE_MAGNETIC_FIELD:
                        magnitude_values = sensorEvent.values.clone();
                        sensorReady = true;
                        break;
                    case Sensor.TYPE_ACCELEROMETER:
                        accelerometer_values = sensorEvent.values.clone();
                        sensorReady = true;
                }*/

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


                Log.v("Accuracy", String.valueOf(LocationObj.getAccuracy()));
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
            //   txt_deg.setText("" + direction + " - (" + bearingText + ")");
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
          /*  random_text.setText("");
            random_text.setText("Latitude :: " + location.getLatitude() + " Longitude :: " + location.getLongitude() + " Accuracy :: " + location.getAccuracy());*/
            LocationObj = location.toLocation();

            count++;

            Log.v("IA Location Changed", location.toString());

            if (!locarray.isEmpty())
            {

                for (int i = 0; i < locarray.size(); i++)
                {
                    if (cnt < locarray.size()) {
                        destinationObj.setLatitude(locarray.get(cnt).getLatitude());
                        destinationObj.setLongitude(locarray.get(cnt).getLongitude());

                        /*txt_lati.setText("Latitude : "+destinationObj.getLatitude());
                        txt_longi.setText("Longitude : "+destinationObj.getLongitude());
*/
                        txt_dis.setText("Distance : " + location.toLocation().distanceTo(destinationObj) + " m");

                        Float dis = location.toLocation().distanceTo(destinationObj);

                        //txt_cnt.setText("Count : "+count);

                        if (Integer.valueOf(dis.intValue()) < 2 && count % 10 == 0) {
                            Toast.makeText(getApplicationContext(), "Reached !", Toast.LENGTH_SHORT).show();

                            cnt++;

                            if (cnt == locarray.size()) {
                                   /* AlertDialog.Builder alertadd = new AlertDialog.Builder(MainActivity.this);
                                    LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                                    final View view = factory.inflate(R.layout.alertdialog, null);
                                    alertadd.setView(view);
                                    alertadd.setNeutralButton("Ok!", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dlg, int sumthin) {
                                            return;
                                        }
                                    });

                                    alertadd.show();*/

                                arrow.setVisibility(View.GONE);
                                finaimg.setVisibility(View.VISIBLE);
                            } else {
                                Snackbar mySnackbar = Snackbar.make(findViewById(R.id.main_act), "Reached!", Snackbar.LENGTH_LONG);

                                mySnackbar.setAction("OK", new MyOkListener());
                                mySnackbar.show();
                            }

                        }
                    }
                }

            }
        }
    };

    public class MyOkListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            return;
        }
    }


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