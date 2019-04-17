package com.example.ttwil.mapsprototype1;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.data.kml.KmlLayer;
import com.suke.widget.SwitchButton;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 1235;
    private static final float DEFAULT_ZOOM = 18;
    private static final int MIN_DISTANCE = 0;
    private static final int MIN_TIME = 1000;
    private static final int MIN_ZOOM = 9;
    private static final int MAX_ZOOM = 17;

    // Variables
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private Boolean isSpraying = false;
    private Boolean isConnected = true;
    private Location lastLocation = null;
    private Circle sprayArea;
    private DrawerLayout mDrawerLayout;
    private boolean isFirstOfSession;
    private Vector<Event> events;
    private boolean isInSession;
    private List<Marker> mMarkers;
    private int sessionNumber;
    private float mph;
    private double totalFlow;
    private double totalNightlyFlow;
    private float flowRate;

    //Preferences
    //Points
    private String pointIcon;
    private int pointDropFrequency;
    private float pointSize;
    private String pointColorOn;
    private String pointColorOff;
    private boolean pointRotation;

    //Swath
    private String swathColor;
    private int swathOpacity;
    private int swathSize;
    private boolean swathIsOn;

    // Route
    private boolean routeIsOn;
    private int routeOpacity;

    // Map
    private boolean mapRotationEnabled;
    private boolean mapEventButtonEnabled;

    // GPS
    private boolean GPSDialEnabled;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // Get permissions
        getLocationPermission();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_REQUEST_CODE);
        }

        // Initialize Variables
        events = new Vector<Event>();
        isFirstOfSession = true;
        isInSession = false;
        mMarkers = new ArrayList<>();
        sessionNumber = 0;
        totalFlow = 0;

        // Initialize flowRate variable
        Bundle extras = getIntent().getExtras();
        if (extras != null) { flowRate = extras.getFloat("flowRate", 0); }

        // Run Initializing Functions
        initializeActionBar();
        initializeDrawerMenu();
        initializeOneSecondTimer();
        initializeButtons();
        initializeSwitch();
    }

    @Override
    protected void onResume() {
        // Update Preferences every time the settings menu is closed
        updatePreferences();
        updateButtons();

        super.onResume();
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: Getting the device's current location");

        // Initialize Location Provider
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful() && task.getResult() != null) { // Location is found
                            Log.d(TAG, "onComplete: Found Location");
                            Location currentLocation = (Location) task.getResult();
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);
                        } else { // Location is not found
                            Log.d(TAG, "onComplete: Current location null");
                            Toast.makeText(MapActivity.this, "Unable to get current Location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: " + e.getMessage());
        }
    }

    // Move camera to specified LatLng and Zoom
    private void moveCamera(LatLng latLng, float zoom) {
        Log.e(TAG, "moveCamera: Moving the camera to: " + latLng.latitude + "," + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    // Initialize Map
    private void initMap() {
        Log.d(TAG, "initMap: Initializing map");
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        // Setup Google Map Object
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
            Log.d(TAG, "Map is ready.");
            Toast.makeText(MapActivity.this, "Map is ready", Toast.LENGTH_SHORT).show();
            mMap = googleMap;

            // Get location if location permissions are granted
            if (mLocationPermissionsGranted) {
                getDeviceLocation();

                // Set map properties
                try {
                    Log.d(TAG, "onMapReady: Setting my location to true");
                    mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                    mMap.addTileOverlay(new TileOverlayOptions().
                            tileProvider(new CustomMapTileProvider()).zIndex(-1));
                    mMap.setMyLocationEnabled(true);
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    mMap.setMinZoomPreference(MIN_ZOOM);
                    mMap.setMaxZoomPreference(MAX_ZOOM);
                    mMap.getUiSettings().setRotateGesturesEnabled(false);

                    // Add Bees, Call befores, No fogs, Organic Farms
                    addMapLayer("No_Spray.kml");
                } catch (SecurityException e) {
                    Log.e(TAG, "onMapReady: Failed to get location" + e.getMessage());
                    //
                    // ADD IN ERROR HANDLING FOR LOCATION NOT FOUND CRASH
                    // (App crashes if location is not found after tablet is reset)
                    // (P.S. I think it happens here, but I'm only like 30% sure)
                    //

                    // Move to login activity if location is not available
                    Intent intent = new Intent(MapActivity.this, LoginActivity.class);
                    startActivity(intent);
                }
            }
            }
        });

        // Initialize Location Manager, return if permissions are not granted
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Setup location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME, MIN_DISTANCE, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Drop a dot/arrow if the spray unit is connected.
                if (isConnected) { handleLocationChange(location); }

                // Update spray area location
                updateSprayArea(location);

                // Update GPS dial accuracy
                updateGPSAccuracy();

                // Update location value
                lastLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                updateGPSAccuracy();
            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

    // Adds a specified KML to the map, on top of map tiles
    private void addMapLayer(String filename) {
        try {
            File file = new File("storage/DE94-B005/Map Layers/" + filename);
            FileInputStream in = new FileInputStream(file);
            KmlLayer layer = new KmlLayer(mMap, in, getApplicationContext());

            layer.addLayerToMap();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    // Ask the user for permission to access location
    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: Getting location permissions");
        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called");
        mLocationPermissionsGranted = false;

        switch(requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:{
                if (grantResults.length > 0) {
                    for (int grantResult : grantResults) {
                        if (grantResult != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    // Initialize Map
                    initMap();
                }
            }
        }
    }

    public float getAngle(Location old, Location current) {
        if (old == null) { return 0; }

        double xDif = current.getLatitude() - old.getLatitude();
        double yDif = current.getLongitude() - old.getLongitude();
        float angle = (float) Math.toDegrees(Math.atan2(yDif, xDif));

        return angle;
    }

    public void handleLocationChange(Location location) {
        if (isInSession) {
            float angle = getAngle(lastLocation, location);
            drawDot(location, angle);
            rotateMap(angle);
            updateTotalFlow();
        }

        if (lastLocation != null) { updateSpeed(location); }

        lastLocation = location;
    }

    private void updateSpeed(Location location) {
        float distance = (float) Math.sqrt(Math.pow((location.getLatitude() - lastLocation.getLatitude()), 2)
                + Math.pow((location.getLongitude() - lastLocation.getLongitude()), 2)) * 364000;
        mph = distance * 60 * 60 / 5280;

        TextView speedText = findViewById(R.id.speedText);
        speedText.setText(String.format("%.1f mph", mph));
    }

    private void updateTotalFlow() {
        if (isInSession) {
            float currentFlow = (flowRate / 10 * mph / 60);
            totalFlow += currentFlow;
        }

        TextView totalFlowText = findViewById(R.id.totalFlowText);
        totalFlowText.setText(String.format("%.2f gal", totalFlow / 128));
    }

    private void rotateMap(float angle) {
    }

    public void drawDot(Location location, float angle) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng)
                     .flat(true);

        Bitmap icon = null;

        try {
            InputStream is;

            if (isFirstOfSession) {
                is = getAssets().open("dot.bmp");
                isFirstOfSession = false;
            } else {
                is = getAssets().open(pointIcon);
            }
            icon = BitmapFactory.decodeStream(is);
            icon = Bitmap.createScaledBitmap(icon, (int)(icon.getWidth()*pointSize),
                    (int)(icon.getHeight()*pointSize), false);

            int[] pixels = new int[icon.getWidth()*icon.getHeight()];
            icon.getPixels(pixels, 0, icon.getWidth(), 0, 0, icon.getWidth(), icon.getHeight());

            int color;

            if (isSpraying) {
                color = Color.parseColor(pointColorOn);
                markerOptions.title("1");
            } else {
                color = Color.parseColor(pointColorOff);
                markerOptions.title("0");
            }

            for (int i = 0; i < pixels.length; i++) {
                if (pixels[i] == -1)
                    pixels[i] = color;
            }

            icon = icon.copy(Bitmap.Config.ARGB_8888, true);
            icon.setPixels(pixels, 0, icon.getWidth(), 0, 0, icon.getWidth(), icon.getHeight());

        } catch (IOException e1) {
            e1.printStackTrace();
        }

        if (icon != null) {
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(icon));
        }

        if (pointRotation) {
            if (lastLocation != null) {
                markerOptions.rotation(angle);
                if (mapRotationEnabled) {
                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))
                            .zoom(mMap.getCameraPosition().zoom)
                            .bearing(angle)
                            .tilt(0)
                            .build();

                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        }

        mMarkers.add(mMap.addMarker(markerOptions));
    }

    private void addEvent(Location location) {
        if (location == null) {
            Toast.makeText(this, "Cannot find current location. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // ADD MARKER
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
        markerOptions.zIndex(999999);

        Marker marker = mMap.addMarker(markerOptions);
        marker.setTag(events.size());

        // ADD EVENT TO VECTOR
        events.add(new Event(new LatLng(location.getLatitude(), location.getLongitude())));

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                if (marker.getTag() != null) {
                    Toast.makeText(MapActivity.this, "Number: " + marker.getTag(), Toast.LENGTH_SHORT).show();
                }
                return false;
            }
        });
    }

    public void startSession() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        isInSession = true;
                        Intent intent = new Intent(MapActivity.this, SessionActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Start a new session?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    public void endSession() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        isInSession = false;
                        updateButtons();

                        KML kml = new KML();
                        try {
                            kml.createKML(mMarkers, sessionNumber);
                            mMarkers = new ArrayList<>();
                            StringBuilder sb = new StringBuilder();

                            sb.append("Time Stop: ");
                            TextView timeStopText = findViewById(R.id.input_time_stop);
                            // sb.append(timeStopText.getText().toString());
                            sb.append("\n");

                            sb.append("Predicted Flow Amount: ");
                            sb.append(totalFlow);
                            sb.append("\n");

                            sb.append("Actual Flow Amount: ");
                            EditText flowInput = findViewById(R.id.input_total_flow);
                            sb.append(flowInput.getText().toString());
                            sb.append("\n");

                            sb.append("Notes: ");
                            EditText notesText = findViewById(R.id.input_notes);
                            sb.append(notesText.getText().toString());
                            sb.append("\n");

                            writeToFile(sb.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(MapActivity.this, "Error creating KML.", Toast.LENGTH_SHORT).show();
                        }

                        sessionNumber++;
                        totalNightlyFlow += totalFlow;
                        totalFlow = 0;

                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_end_session, null));

        builder.setMessage("End current session?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }
    
    @RequiresApi(api = Build.VERSION_CODES.O)
    private void logOff() {
        if (isInSession) {
            Toast.makeText(this, "You must end the session before logging out.", Toast.LENGTH_SHORT).show();
            return;
        }

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        StringBuilder sb = new StringBuilder();
                        sb.append("Time Returning to Compound: ");
                        Date time = Calendar.getInstance().getTime();
                        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a");
                        sb.append(sdf.format(time));
                        sb.append("\n");

                        writeToFile(sb.toString());

                        Toast.makeText(MapActivity.this, "Logging out", Toast.LENGTH_SHORT).show();
                        finishAffinity();
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_logout, null));

        builder.setMessage("Log Out").setPositiveButton("Log Out", dialogClickListener)
                .setNegativeButton("Cancel", dialogClickListener).show();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void writeToFile(String text) {
        File dataFile = new File(Environment.getExternalStorageDirectory(), "Login Data/session_login.txt");
        if (!dataFile.exists()) { return; }

        try {
            Files.write(dataFile.toPath(), text.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void updateButtons() {
        if (isInSession) {
            findViewById(R.id.btnNewSession).setVisibility(View.INVISIBLE);
            findViewById(R.id.btnEndSession).setVisibility(View.VISIBLE);
            findViewById(R.id.onOffSwitch).setVisibility(View.VISIBLE);
            findViewById(R.id.onLabel).setVisibility(View.VISIBLE);
            findViewById(R.id.offLabel).setVisibility(View.VISIBLE);
            findViewById(R.id.totalFlowText).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.btnNewSession).setVisibility(View.VISIBLE);
            findViewById(R.id.btnEndSession).setVisibility(View.INVISIBLE);
            findViewById(R.id.onOffSwitch).setVisibility(View.INVISIBLE);
            findViewById(R.id.onLabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.offLabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.totalFlowText).setVisibility(View.INVISIBLE);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public void updatePreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // POINT
        pointIcon = sharedPreferences.getString("point_icon", "arrow.bmp");
        pointColorOn = sharedPreferences.getString("point_color_on", "#000000");
        pointColorOff = sharedPreferences.getString("point_color_off", "#000000");
        pointDropFrequency = Integer.parseInt(sharedPreferences.getString("point_drop_frequency", "1"));
        pointRotation = sharedPreferences.getBoolean("point_rotation", false);
        pointSize = Float.parseFloat(sharedPreferences.getString("point_size", "1"));

        // SWATH
        swathColor = sharedPreferences.getString("swath_color_list", "#000000");
        swathOpacity = Integer.parseInt(sharedPreferences.getString("swath_opacity_list", "15"));
        swathSize = Integer.parseInt(sharedPreferences.getString("swath_size", "150"));
        swathIsOn = sharedPreferences.getBoolean("swath_on_off", true);

        // ROUTE
        routeIsOn = sharedPreferences.getBoolean("route_on_off", false);
        routeOpacity = Integer.parseInt(sharedPreferences.getString("route_opacity_list", "15"));

        // MAP
        mapEventButtonEnabled = sharedPreferences.getBoolean("maps_events_button", true);
        mapRotationEnabled = sharedPreferences.getBoolean("maps_rotation", false);

        // GPS
        GPSDialEnabled = sharedPreferences.getBoolean("gps_show_accuracy", true);
    }

    private void updateGPSAccuracy() {
        if (!mLocationPermissionsGranted || lastLocation == null) { return; }

        int accuracy = 100 - (int) lastLocation.getAccuracy();

        CustomGauge gauge = findViewById(R.id.GPSGauge);
        gauge.setValue(accuracy);

        // Change color of gauge
        if (accuracy > 66) {
            gauge.setPointEndColor(getResources().getColor(R.color.lightGreen));
            gauge.setPointStartColor(getResources().getColor(R.color.lightGreen));
        } else if (accuracy > 33) {
            gauge.setPointEndColor(getResources().getColor(R.color.yellow));
            gauge.setPointStartColor(getResources().getColor(R.color.yellow));
        } else {
            gauge.setPointEndColor(getResources().getColor(R.color.darkRed));
            gauge.setPointStartColor(getResources().getColor(R.color.darkRed));
        }

        // Update gauge text
        TextView gaugeText = findViewById(R.id.gaugeText);
        gaugeText.setText(accuracy + "%");
    }

    public void showTimePickerDialog(View view) {
        DialogFragment dialogFragment = TimePickerFragment.newInstance(R.id.input_time_stop);
        dialogFragment.show(getSupportFragmentManager(), "timePicker");
    }

    private void initializeOneSecondTimer() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Runs every second
            }
        }, 0, 1000);
    }

    private void initializeDrawerMenu() {
        mDrawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                        CharSequence i = menuItem.getTitle();
                        if ("Settings".contentEquals(i)) {
                            Intent intent = new Intent(MapActivity.this, SettingsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else if ("Form".contentEquals(i)) {
                            Toast.makeText(MapActivity.this, "Congratulations: You clicked the Form Button.", Toast.LENGTH_SHORT).show();
                        } else if ("Events".contentEquals(i)) {
                            Intent intent = new Intent(MapActivity.this, EventsActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        } else {
                            Toast.makeText(MapActivity.this, "ERROR: Tried to access a menu item that does not exist.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                }
        );



    }

    private void initializeSwitch() {
        final SwitchButton onOffSwitch = findViewById(R.id.onOffSwitch);
        onOffSwitch.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                isSpraying = onOffSwitch.isChecked();
            }
        });
    }

    private void initializeButtons() {
        // Event Button
        ImageButton btnEvent = findViewById(R.id.btnEvent);
        btnEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addEvent(lastLocation);
            }
        });

        // New Session Button
        ImageButton btnNewSession = findViewById(R.id.btnNewSession);
        btnNewSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSession();
            }
        });

        // End Session Button
        ImageButton btnEndSession = findViewById(R.id.btnEndSession);
        btnEndSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endSession();
            }
        });

        // Drawer Button
        ImageButton btnDrawer = findViewById(R.id.drawerButton);
        btnDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(Gravity.RIGHT);
            }
        });
        
        // Log Out Button
        TextView btnLogOut = findViewById(R.id.logOutButton);
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logOff();
            }
        });
    }

    private void initializeActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        View customView = getLayoutInflater().inflate(R.layout.custom_action_bar, null);
        actionBar.setCustomView(customView);
        Toolbar parent = (Toolbar) customView.getParent();
        parent.setPadding(0,0,0,0);

        if (Build.VERSION.SDK_INT > 21) {
            parent.setContentInsetsAbsolute(0, 0);
        }

        actionBar.setElevation(0);
    }

    private void updateSprayArea(Location location) {
        if (swathIsOn) {
            if (sprayArea != null) {
                sprayArea.setCenter(new LatLng(location.getLatitude(), location.getLongitude()));
                sprayArea.setRadius(swathSize / 3.28);

                String fillColor = swathColor;
                fillColor = fillColor.substring(1);
                fillColor = "#" + (Integer.toString(swathOpacity)) + fillColor;

                sprayArea.setFillColor(Color.parseColor(fillColor));

            } else {
                //Initialize spray area circle
                CircleOptions circleOptions = new CircleOptions()
                        .center(new LatLng(0, 0))
                        .radius(swathSize / 3.28)
                        .strokeWidth(0)
                        .fillColor(Color.WHITE)
                        .zIndex(999999);

                sprayArea = mMap.addCircle(circleOptions);
            }
        }
    }

    @Override
    public void onBackPressed() {
        mDrawerLayout.closeDrawer(Gravity.RIGHT);
    }
}
