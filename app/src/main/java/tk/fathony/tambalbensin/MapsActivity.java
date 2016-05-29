package tk.fathony.tambalbensin;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends ActionBarActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.InfoWindowAdapter, GoogleMap.OnMarkerClickListener ,
        GoogleMap.OnInfoWindowClickListener{

    private static final int REQUEST_PERMISSION_LOCATION = 0;
    private GoogleApiClient mGoogleApiClient;
    private Location myLocation;
    private GoogleMap map;
    private ArrayList<Bundle> data = new ArrayList<>();
    private ArrayList<Marker> mMarker = new ArrayList<>();
    private String jenis;
    private View mInfoWindow;
    private String DIRECTION_URL = "https://maps.googleapis.com/maps/api/directions/json?";
    private Polyline curPolyline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        MapView map = (MapView) findViewById(R.id.map);

        assert map != null;
        map.onCreate(savedInstanceState);
        map.onResume();

        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }
        map.getMapAsync(this);


        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        Bundle data = getIntent().getExtras();
        String url = data.getString("url");
        InternetConnection ic = new InternetConnection(this, true) {
            @Override
            protected void OnSuccess(String result) throws JSONException {
                parseData(new JSONArray(result));
            }
        };
        ic.get(url);
        jenis = data.getString("jenis");
        ActionBar ab = getSupportActionBar();
        if(ab!=null){
            ab.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
            ab.setTitle(jenis.equals("pom")?"Pom Bensin":"Tambal Ban");
            ab.setIcon(getResources().getDrawable(jenis.equals("pom")?
                    R.drawable.marker_pom:R.drawable.marker_ban));

        }
        mInfoWindow = getLayoutInflater().inflate(R.layout.infowindow,null);
    }

    private void parseData(JSONArray result){
        try{
            for(int i=0;i<result.length();i++){
                JSONObject obj = result.getJSONObject(i);
                Bundle row = new Bundle();
                row.putInt("id",obj.getInt("id"));
                row.putString("nama",obj.getString("nama"));
                row.putString("alamat",obj.getString("alamat"));
                row.putString("keterangan",obj.getString("keterangan"));
                row.putString("latitude",obj.getString("latitude"));
                row.putString("longitude",obj.getString("longitude"));
                data.add(row);
            }
        }catch (JSONException ex){ ex.printStackTrace();}
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                drawMarker();
            }
        });

    }

    private void drawMarker(){
        if(data.isEmpty() || map==null) return;

        for(Bundle d: data){
            Marker m = map.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(d.getString("latitude")),
                    Double.parseDouble(d.getString("longitude"))))
                    .title(d.getString("nama"))
                    .snippet(d.getString("keterangan"))
                    .icon(BitmapDescriptorFactory.fromResource(
                            jenis.equals("pom")?R.drawable.marker_pom:R.drawable.marker_ban))
            );
            mMarker.add(m);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        LatLng sidoarjo = new LatLng(-7.45,112.7);
        CameraPosition providerLocation =
                new CameraPosition.Builder().target(sidoarjo)
                        .zoom(14)
                        .build();
        map.moveCamera(CameraUpdateFactory.newCameraPosition(providerLocation));

        drawMarker();
        map.setOnMarkerClickListener(this);
        map.setInfoWindowAdapter(this);
        map.setOnInfoWindowClickListener(this);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this,"Maaf aplikasi tidak bisa berjalan tanpa izin lokasi.",Toast.LENGTH_LONG)
                            .show();
                }
                return;
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
        accessLocation(connectionHint);
    }

    private void accessLocation(Bundle connectionHint){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQUEST_PERMISSION_LOCATION);
            return;
        }
        myLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        if (myLocation != null) {
            MarkerOptions opt = new MarkerOptions();
            opt.position(new LatLng(myLocation.getLatitude(), myLocation.getLongitude()));
            opt.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_me));
            opt.visible(true);
            map.addMarker(opt);

            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(myLocation.getLatitude(), myLocation
                            .getLongitude()), 17));
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public View getInfoWindow(Marker marker) {
        for(int i=0;i<mMarker.size();i++){
            if(mMarker.get(i).equals(marker)){
                return render(i);
            }
        }
        return null;
    }

    private View render(int i){
        Bundle b = data.get(i);

        ((TextView)mInfoWindow.findViewById(R.id.title)).setText(b.getString("nama"));
        ((TextView)mInfoWindow.findViewById(R.id.alamat)).setText(b.getString("alamat"));

        ((TextView)mInfoWindow.findViewById(R.id.keterangan)).setText(Html.fromHtml(b.getString("keterangan")));
        return mInfoWindow;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        if (curPolyline != null) {
            curPolyline.remove();
        }
        for(int i=0;i<mMarker.size();i++){
            if(mMarker.get(i).equals(marker)){
                marker.hideInfoWindow();
                requestDirection(i);
            }
        }
    }

    private void requestDirection(int i) {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        Bundle d = data.get(i);
        String origin = "origin="+myLocation.getLatitude()+","+myLocation.getLongitude()+"&";
        String destination = "destination="+d.getString("latitude")+","+d.getString("longitude");

        String  url = DIRECTION_URL+origin+destination;
        InternetConnection ic = new InternetConnection(this, true) {
            @Override
            protected void OnSuccess(String result) throws JSONException {
                final JSONObject obj = new JSONObject(result);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        parseJSON(obj);
                    }
                });
            }
        };
        ic.get(url);
    }

    private void parseJSON(JSONObject obj) {
        DirectionsJSONParser parser = new DirectionsJSONParser();
        List<List<HashMap<String, String>>> routes = parser.parse(obj);
        drawRoutes(routes);
    }

    private void drawRoutes(List<List<HashMap<String, String>>> routes){
        ArrayList<LatLng> points = null;
        PolylineOptions lineOptions = null;

        // Traversing through all the routes
        for (int i = 0; i < routes.size(); i++) {
            points = new ArrayList<LatLng>();
            lineOptions = new PolylineOptions();

            // Menginisialisasi i-th route
            List<HashMap<String, String>> path = routes.get(i);

            // Fetching all the points in i-th route
            for (int j = 0; j < path.size(); j++) {
                HashMap<String, String> point = path.get(j);

                double lat = Double.parseDouble(point.get("lat"));
                double lng = Double.parseDouble(point.get("lng"));
                LatLng position = new LatLng(lat, lng);

                points.add(position);
            }

            // Menambahkan semua points dalam rute ke LineOptions
            lineOptions.addAll(points);
            lineOptions.width(4);
            lineOptions.color(jenis.equals("pom")?Color.YELLOW:Color.BLUE);
        }

        curPolyline = map.addPolyline(lineOptions);
        findViewById(R.id.loading).setVisibility(View.GONE);
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }
}
