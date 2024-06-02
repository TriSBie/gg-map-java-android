package com.example.mapdemo;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Location;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.util.Log;
import android.widget.Toolbar;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.PolylineOptions;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnMyLocationClickListener {
    private GoogleMap mMap = null;
    private PlacesClient placesClient = null;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private AutocompleteSupportFragment autocompleteFragment = null;
    private AutocompleteSupportFragment autocompleteFragmentEndPoint = null;
    private LatLng startLatLng, endLatLng;
    private final String apiKey = "AIzaSyDuPzcK7RYm5e0hAufDTsXpdPjubKe-7tI";
    ArrayList markerPoints = new ArrayList();

    @SuppressLint("UseSupportActionBar")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setActionBar(toolbar);

        // get the API key from the AndroidManifest.xml file
        Places.initialize(getApplicationContext(), apiKey);
        placesClient = Places.createClient(this);

        // initialize the autocomplete fragment and map fragment
        autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                LatLng latLng = place.getLatLng();
                if (latLng == null) {
                    System.out.println("No latlng found for place: " + place.getName());
                    return;
                }
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(latLng).title(place.getName()));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
            }

            @Override
            public void onError(@NonNull Status status) {
                Log.i("MainActivity", "An error occurred: " + status);
                Toast.makeText(MainActivity.this, "An error occurred: " + status, Toast.LENGTH_SHORT).show();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this); // calls onMapReady
        }
        autocompleteFragment = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        ;
        autocompleteFragmentEndPoint = (AutocompleteSupportFragment) getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment_v1);
        ;
        setupAutocompleteFragment(R.id.autocomplete_fragment, true);
        setupAutocompleteFragment(R.id.autocomplete_fragment_v1, false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.mapNone){
            mMap.setMapType(GoogleMap.MAP_TYPE_NONE);
        }
        if(id == R.id.mapNormal){
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        if(id == R.id.mapSattelite){
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        }
        if(id == R.id.mapHybrid){
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }
        if(id == R.id.mapTerrain){
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupAutocompleteFragment(int fragmentId, boolean isStartPoint) {
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(fragmentId);
        if (autocompleteFragment != null) {
            autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG));
            autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    LatLng selectedLatLng = place.getLatLng();
                    if (selectedLatLng != null) {
                        if (isStartPoint) {
                            startLatLng = selectedLatLng;
                            mMap.addMarker(new MarkerOptions().position(startLatLng).title("Start Point"));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15));
                        } else {
                            endLatLng = selectedLatLng;
                            mMap.addMarker(new MarkerOptions().position(endLatLng).title("End Point"));
                            if (startLatLng != null) {
                                drawRoute(startLatLng, endLatLng);
                            }
                        }
                    }
                }

                @Override
                public void onError(@NonNull com.google.android.gms.common.api.Status status) {
                    Toast.makeText(MainActivity.this, "Error: " + status.getStatusMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // Optional: Add a marker in a default location and move the camera
        LatLng defaultLocation = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(defaultLocation).title("Default Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(defaultLocation));
        // set image more detail
        // Enable buildings
        mMap.setBuildingsEnabled(true);

        // setting some configuration styles
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnMyLocationClickListener(this);

        // Enable traffic
        mMap.setTrafficEnabled(true);

        // Enable indoor maps
        mMap.setIndoorEnabled(true);

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style));

            if (!success) {
                Log.e("MainActivity", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MainActivity", "Can't find style. Error: ", e);
        }

        // enable mylocation function
        enableMyLocation();
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    @SuppressLint("MissingPermission") // Suppressing missing permission warning
    private void enableMyLocation() {
        // [START maps_check_location_permission]
        // 1. Check if permissions are granted, if so, enable the my location layer
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            return;
        }

        // 2. Otherwise, request location permissions from the user.
        PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, true);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            Log.i("MainActivity", "Permission was denied");
            Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show();
            // [END_EXCLUDE]
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        // Return false so that we don't consume the event and the default behavior still occurs
        // (the camera animates to the user's current position).
        return false;
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Toast.makeText(this, "Current location:\n" + location, Toast.LENGTH_LONG).show();
    }


    // DRAWABLE ROUTE
    private void drawRoute(LatLng start, LatLng end) {
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin=" + start.latitude + "," + start.longitude +
                "&destination=" + end.latitude + "," + end.longitude + "&key=" + apiKey;

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonResponse = new JSONObject(response);
                    JSONArray routes = jsonResponse.getJSONArray("routes");
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedString = overviewPolyline.getString("points");
                    List<LatLng> points = decodePoly(encodedString);

                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(points)
                            .color(Color.BLUE)
                            .width(10);
                    mMap.addPolyline(polylineOptions);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Error fetching route", Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue.add(stringRequest);
    }

    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }


}