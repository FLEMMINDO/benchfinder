package com.example.bench;


import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Menu;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import com.example.bench.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.database.sqlite.SQLiteDatabase;
import android.widget.CompoundButton;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private MapView map;
    IMapController mapController;
    LocationManager lm;
    GeoPoint gp;

//    private void RefreshMap(ArrayList<OverlayItem> items) {
//        ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(getApplicationContext(),
//                items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
//            @Override
//            public boolean onItemSingleTapUp(int index, OverlayItem item) {
//                return true;
//            }
//
//            @Override
//            public boolean onItemLongPress(int index, OverlayItem item) {
//                return false;
//            }
//        });
//
//        mOverlay.setFocusItemsOnTap(true);
//        map.getOverlays().remove(0);
//        map.getOverlays().add(0, mOverlay);
//    }
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("R.string.title_location_permission")
                        .setMessage("R.string.text_location_permission")
                        .setPositiveButton("R.string.ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400, 1, (LocationListener) this);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

        }
    }
    private Location getLastKnownLocation() {
        List<String> providers = lm.getProviders(true);
        Location bestLocation = null;
        checkLocationPermission();
        for (String provider : providers) {
            Location l = lm.getLastKnownLocation(provider);

            if (l == null) {
                continue;
            }
            if (bestLocation == null
                    || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }
    public static double getDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(getApplicationContext(),
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);//render
        map.setBuiltInZoomControls(true);//roomable
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLoc = getLastKnownLocation();
        if (lastKnownLoc != null){
            float longTemp = (float) lastKnownLoc.getLongitude();
            float latTemp = (float) lastKnownLoc.getLatitude();
            gp =  new GeoPoint(latTemp,longTemp);
        }
        else
            gp = new GeoPoint(55.99448173922669,92.79750509183452);
        GeoPoint startPoint = gp;
        IMapController mapController = map.getController();
        mapController.setZoom(18.0);
        mapController.setCenter(startPoint);
        Marker mymarker = new Marker(map);
        mymarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mymarker.setTitle("Ты здесь!");
        Drawable myd = ResourcesCompat.getDrawable(getResources(), R.mipmap.you1, null);
        Bitmap mybitmap = ((BitmapDrawable) myd).getBitmap();
        Drawable mydr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(mybitmap, (int) (48.0f * getResources().getDisplayMetrics().density), (int) (48.0f * getResources().getDisplayMetrics().density), true));
        mymarker.setPosition(startPoint);

        ArrayList<OverlayItem> items =new ArrayList<>();
        mymarker.setIcon(mydr);
        map.getOverlays().add(mymarker);
        map.invalidate();
        //OverlayItem home =new OverlayItem(mymarker.getTitle(),mymarker.getSnippet(),mymarker.getPosition());
        //items.add(home);
        //items.add(new OverlayItem("asda","asdda", new GeoPoint(56.064696,92.932415)));

        ItemizedOverlayWithFocus<OverlayItem> mOverlay= new ItemizedOverlayWithFocus<OverlayItem>(getApplicationContext(),
                items, new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
            @Override
            public boolean onItemSingleTapUp(int index, OverlayItem item) {
                return true;
            }

            @Override
            public boolean onItemLongPress(int index, OverlayItem item) {
                return false;
            }
        });
        mOverlay.setFocusItemsOnTap(true);
        map.getOverlays().add(mOverlay);

        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);



//----------------------------------------------------------------


        SQLiteDatabase db = getBaseContext().openOrCreateDatabase("benchfinder.sqlite", MODE_PRIVATE, null);

        String aboba = "CREATE TABLE IF NOT EXISTS \"benches\" (\n" +
                "\t\"ID\"\tINTEGER NOT NULL UNIQUE,\n" +
                "\t\"coord_x\"\tREAL,\n" +
                "\t\"coord_y\"\tREAL,\n" +
                "\t\"photo\"\tTEXT,\n" +
                "\t\"upvotes\"\tINTEGER,\n" +
                "\t\"downvotes\"\tINTEGER,\n" +
                "\t\"ID_uploader\"\tINTEGER,\n" +
                "\tPRIMARY KEY(\"ID\" AUTOINCREMENT)\n" +
                ");";
        db.execSQL(aboba);

//        db.execSQL("insert into benches (coord_x,coord_y,photo,upvotes,downvotes,ID_uploader) values (212.23543245,345.3453456,'aboba',90,1337,0)");

        final double[] selected_coords = {0, 0};
        final double[] my_coords = {0, 0};

        TextView textView = findViewById(R.id.text_home228);

        textView.setText("");
        //db.execSQL("delete * from benches");
        Cursor query = db.rawQuery("SELECT * FROM benches;", null);
//        while(query.moveToNext()){
//            int id = query.getInt(0);
//            float coord_x = query.getFloat(1);
//            float coord_y = query.getFloat(2);
//            //textView.append("id: " + id + " coord_x: " + coord_x + "coord_y" + coord_y + "\n");
//        }
        Marker dot = new Marker(map);
        Drawable myd1 = ResourcesCompat.getDrawable(getResources(), R.drawable.touch1, null);
        Bitmap mybitmap1 = ((BitmapDrawable) myd1).getBitmap();
        Drawable mydr1 = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(mybitmap1, (int) (12 * getResources().getDisplayMetrics().density), (int) (12 * getResources().getDisplayMetrics().density), true));
        dot.setIcon(mydr1);
        MyLocationNewOverlay locationOverlay = new MyLocationNewOverlay(map);
        locationOverlay.enableFollowLocation();
        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                dot.setPosition(p);
                map.getOverlays().add(dot);
                map.invalidate();
                selected_coords[0] = p.getLatitude();
                selected_coords[1] = p.getLongitude();
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        query = db.rawQuery("SELECT * FROM benches;", null);
        items.clear();
        ArrayList<Float> dist =new ArrayList<>();
        dist.clear();
        while(query.moveToNext()){
            int id = query.getInt(0);
            float coord_x = query.getFloat(1);
            float coord_y = query.getFloat(2);
            String text = query.getString(3);
            int upvotes = query.getInt(4);
            int downvotes = query.getInt(5);
            int id_uploader = query.getInt(6);
            float distance = (float) getDistance((float) lastKnownLoc.getLatitude(),(float) lastKnownLoc.getLongitude(),coord_x,coord_y);
            dist.add(distance);
            Marker marker = new Marker(map);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setTitle(text);
            Drawable d = ResourcesCompat.getDrawable(getResources(), R.mipmap.benchicon2, null);
            Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
            Drawable dr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, (int) (48.0f * getResources().getDisplayMetrics().density), (int) (48.0f * getResources().getDisplayMetrics().density), true));
            marker.setPosition(new GeoPoint(coord_x, coord_y));
            marker.setIcon(dr);
            marker.setTitle("Привет, я твоя лавочка!"+" "+distance);
            map.getOverlays().add(marker);
            map.invalidate();
            items.add(new OverlayItem(marker.getTitle(), marker.getSnippet(), marker.getPosition()));
        }
//        RefreshMap(items);
        binding.appBarMain.addPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String sql = "insert into benches (coord_x,coord_y,photo,upvotes,downvotes,ID_uploader) values ("+selected_coords[0]+","+selected_coords[1]+",'Привет, я твоя лавочка!',0,0,0)";
                db.execSQL(sql);
                Cursor query = db.rawQuery("SELECT * FROM benches;", null);
                ArrayList<OverlayItem> its = new ArrayList<>();
                while(query.moveToNext()){
                    int id = query.getInt(0);
                    float coord_x = query.getFloat(1);
                    float coord_y = query.getFloat(2);
                    String text = query.getString(3);
                    int upvotes = query.getInt(4);
                    int downvotes = query.getInt(5);
                    int id_uploader = query.getInt(6);
                    //abobaview.append(" Lat: " + coord_x + " Lon: " + coord_y + "\n");
                    float distance = (float) getDistance((float) lastKnownLoc.getLatitude(),(float) lastKnownLoc.getLongitude(),coord_x,coord_y);
                    dist.add(distance);
                    Marker marker = new Marker(map);
                    marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    marker.setTitle("Привет,я твоя лавочка!"+" "+distance);
                    Drawable d = ResourcesCompat.getDrawable(getResources(), R.mipmap.benchicon2, null);
                    Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                    Drawable dr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, (int) (48.0f * getResources().getDisplayMetrics().density), (int) (48.0f * getResources().getDisplayMetrics().density), true));
                    marker.setIcon(dr);
                    map.getOverlays().remove(dot);
                    map.getOverlays().add(marker);
                    map.invalidate();
                    marker.setPosition(new GeoPoint(coord_x,coord_y));
                    its.add(new OverlayItem(marker.getTitle(), marker.getSnippet(), marker.getPosition()));
                }
//                RefreshMap(its);

            }
        });

        binding.appBarMain.myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mapController.setZoom(18.0);
                mapController.setCenter(startPoint);
//                RefreshMap(its);

            }
        });

        binding.appBarMain.closestBench.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                float mindist = Collections.min(dist);
                Cursor a = db.rawQuery("SELECT * FROM benches;", null);
                while(a.moveToNext()){
                    int id = a.getInt(0);
                    float coord_x = a.getFloat(1);
                    float coord_y = a.getFloat(2);
                    float distance = (float) getDistance((float) lastKnownLoc.getLatitude(),(float) lastKnownLoc.getLongitude(),coord_x,coord_y);
                    GeoPoint b =new GeoPoint(coord_x,coord_y);
                    List<GeoPoint> pts = new ArrayList<>();
                    if (distance==mindist)
                    {
                        mapController.setZoom(20.5);
                        mapController.setCenter(b);

                    }
                }
            }
        });
        binding.appBarMain.m100.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                {
                    Cursor query = db.rawQuery("SELECT * FROM benches;", null);
                    items.clear();
                    ArrayList<Float> dist =new ArrayList<>();
                    dist.clear();
                    while(query.moveToNext()){
                        int id = query.getInt(0);
                        double coord_x = query.getFloat(1);
                        double coord_y = query.getFloat(2);
                        String text = query.getString(3);
                        int upvotes = query.getInt(4);
                        int downvotes = query.getInt(5);
                        int id_uploader = query.getInt(6);
                        float[] results = new float[3];
                        Location.distanceBetween(startPoint.getLatitude(),startPoint.getLongitude(),coord_x,coord_y,results);
                        if (results[0]<=100)
                        {
                            Marker marker = new Marker(map);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setTitle(text);
                            Drawable d = ResourcesCompat.getDrawable(getResources(), R.mipmap.benchicon3, null);
                            Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                            Drawable dr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, (int) (48.0f * getResources().getDisplayMetrics().density), (int) (48.0f * getResources().getDisplayMetrics().density), true));
                            marker.setPosition(new GeoPoint(coord_x, coord_y));
                            marker.setIcon(dr);
                            marker.setTitle("Привет, я твоя лавочка!");
                            map.getOverlays().add(marker);
                            map.invalidate();
                            items.add(new OverlayItem(marker.getTitle(), marker.getSnippet(), marker.getPosition()));
                        }
                        mapController.setZoom(19.0);
                        mapController.setCenter(startPoint);
                    }
                }
                else
                {
                    Cursor query = db.rawQuery("SELECT * FROM benches;", null);
                    items.clear();
                    ArrayList<Float> dist =new ArrayList<>();
                    dist.clear();
                    while(query.moveToNext()){
                        int id = query.getInt(0);
                        double coord_x = query.getFloat(1);
                        double coord_y = query.getFloat(2);
                        String text = query.getString(3);
                        int upvotes = query.getInt(4);
                        int downvotes = query.getInt(5);
                        int id_uploader = query.getInt(6);
                        Marker marker = new Marker(map);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marker.setTitle(text);
                        Drawable d = ResourcesCompat.getDrawable(getResources(), R.mipmap.benchicon2, null);
                        Bitmap bitmap = ((BitmapDrawable) d).getBitmap();
                        Drawable dr = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, (int) (48.0f * getResources().getDisplayMetrics().density), (int) (48.0f * getResources().getDisplayMetrics().density), true));
                        marker.setPosition(new GeoPoint(coord_x, coord_y));
                        marker.setIcon(dr);
                        marker.setTitle("Привет, я твоя лавочка!");
                        map.getOverlays().add(marker);
                        map.invalidate();
                        items.add(new OverlayItem(marker.getTitle(), marker.getSnippet(), marker.getPosition()));
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }


}