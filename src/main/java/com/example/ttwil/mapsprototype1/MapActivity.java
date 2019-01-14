package com.example.ttwil.mapsprototype1;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class MapActivity extends AppCompatActivity {

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 18;
    private static final int MIN_DISTANCE = 0;

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

        getLocationPermission();

        // Initialize Switch listener
        Switch onOffSwitch = findViewById(R.id.onOffSwitch);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSpraying = isChecked;
            }

        });

        // Initialize Drawer menu
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
                            Toast.makeText(MapActivity.this, "You clicked Events. Interesting Choice.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MapActivity.this, "ERROR: Tried to access a menu item that does not exist.", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                }
        );

        isFirstOfSession = true;
    }

    @Override
    protected void onResume() {
        updatePreferences();
        super.onResume();
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: Getting the device's current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: Found Location");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);

                        } else {
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

    private void moveCamera(LatLng latLng, float zoom) {
        Log.e(TAG, "moveCamera: Moving the camera to: " + latLng.latitude + "," + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void initMap() {
        Log.d(TAG, "initMap: Initializing map");
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Log.d(TAG, "Map is ready.");
                Toast.makeText(MapActivity.this, "Map is ready", Toast.LENGTH_SHORT).show();
                mMap = googleMap;

                if (mLocationPermissionsGranted) {
                    getDeviceLocation();

                    try {
                        Log.d(TAG, "onMapReady: Setting my location to true");
                        mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
                        mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new CustomMapTileProvider(getResources().getAssets())));

                        mMap.setMyLocationEnabled(true);
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        mMap.getUiSettings().setZoomControlsEnabled(true);

                    } catch (SecurityException e) {
                        Log.e(TAG, "onMapReady: Failed to get location" + e.getMessage());
                    }
                }
            }
        });

        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, MIN_DISTANCE, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                //Drop a dot/arrow if the spray unit is connected.
                if (isConnected) { drawDotAtCurrentLocation(location); }

                // Update spray area location
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
                                .fillColor(Color.WHITE);

                        sprayArea = mMap.addCircle(circleOptions);
                    }
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        });
    }

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

    public void drawDotAtCurrentLocation(Location location) {
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
                // MAKE THE ICON GREEN
                color = Color.parseColor(pointColorOn);
            } else {
                // MAKE THE ICON RED
                color = Color.parseColor(pointColorOff);
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
                double xDif = location.getLatitude() - lastLocation.getLatitude();
                double yDif = location.getLongitude() - lastLocation.getLongitude();
                float angle = (float) Math.toDegrees(Math.atan2(yDif, xDif));

                markerOptions.rotation(angle);
                lastLocation = location;
            } else {
                lastLocation = location;
            }
        }

        mMap.addMarker(markerOptions);
    }

    public void updatePreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // POINT
        pointIcon = sharedPreferences.getString("point_icon", "arrow.bmp");
        pointColorOn = sharedPreferences.getString("point_color_on", "#000000");
        pointColorOff = sharedPreferences.getString("point_color_off", "#000000");
        pointDropFrequency = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString("point_drop_frequency", "1")));
        pointRotation = sharedPreferences.getBoolean("point_rotation", false);
        pointSize = Float.parseFloat(Objects.requireNonNull(sharedPreferences.getString("point_size", "1")));

        // SWATH
        swathColor = sharedPreferences.getString("swath_color_list", "#000000");
        swathOpacity = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString("swath_opacity_list", "15")));
        swathSize = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString("swath_size", "150")));
        swathIsOn = sharedPreferences.getBoolean("swath_on_off", true);

        // ROUTE
        routeIsOn = sharedPreferences.getBoolean("route_on_off", false);
        routeOpacity = Integer.parseInt(Objects.requireNonNull(sharedPreferences.getString("route_opacity_list", "15")));

        // MAP
        mapEventButtonEnabled = sharedPreferences.getBoolean("maps_events_button", true);
        mapRotationEnabled = sharedPreferences.getBoolean("maps_rotation", false);

        // GPS
        GPSDialEnabled = sharedPreferences.getBoolean("gps_show_accuracy", true);
    }
}
