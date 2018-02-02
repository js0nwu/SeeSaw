package edu.gatech.ubicomp.whoosh.wearapp_maps;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.DismissOverlayView;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import java.io.IOException;
import java.io.InputStream;

import edu.gatech.ubicomp.whoosh.recognizer.AudioService;
import edu.gatech.ubicomp.whoosh.recognizer.SimpleRecognizer;
import edu.gatech.ubicomp.whoosh.recognizer.TrainedRecognizer;

import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_SATELLITE;
import static com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL;

public class MapsActivity extends Activity implements OnMapReadyCallback,
        GoogleMap.OnMapLongClickListener {

    /**
     * Overlay that shows a short help text when first launched. It also provides an option to
     * exit the app.
     */
    private DismissOverlayView mDismissOverlay;

    private final float MAX_ZOOM = 17.0F;
    private final float MIN_ZOOM = 3.0F;

    private final float DEFAULT_ZOOM = 12.0F;

    private final float ZOOM_INCREMENT = 1.0F;
    private final float SCROLL_INCREMENT = 100.0F;

    private final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    private AudioDispatcher dispatcher;

    private Runnable mapZoomIn = new Runnable() {
        @Override
        public void run() {
            Log.v("zoom", "zoom in");
            float currentZoom = mMap.getCameraPosition().zoom;
            mMap.animateCamera(CameraUpdateFactory.zoomTo(Math.min(MAX_ZOOM, currentZoom + ZOOM_INCREMENT)));
        }
    };

    private Runnable mapZoomOut = new Runnable() {
        @Override
        public void run() {
            Log.v("zoom", "zoom out");
            float currentZoom = mMap.getCameraPosition().zoom;
            mMap.animateCamera(CameraUpdateFactory.zoomTo(Math.max(MIN_ZOOM, currentZoom - ZOOM_INCREMENT)));
        }
    };

    private Runnable mapPanNorth = new Runnable() {
        @Override
        public void run() {
            mMap.animateCamera(CameraUpdateFactory.scrollBy(0, -SCROLL_INCREMENT));
        }
    };

    private Runnable mapPanSouth = new Runnable() {
        @Override
        public void run() {
            mMap.animateCamera(CameraUpdateFactory.scrollBy(0, SCROLL_INCREMENT));
        }
    };

    private Runnable mapPanEast = new Runnable() {
        @Override
        public void run() {
            mMap.animateCamera(CameraUpdateFactory.scrollBy(SCROLL_INCREMENT, 0));
        }
    };

    private Runnable mapPanWest = new Runnable() {
        @Override
        public void run() {
            mMap.animateCamera(CameraUpdateFactory.scrollBy(-SCROLL_INCREMENT, 0));
        }
    };

    private Runnable mapReCenter = new Runnable() {
        @Override
        public void run() {
            LatLng sydney = new LatLng(-34, 151);
            mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
        }
    };

    private int mapViewCounter = 0;

    private int[] mapViews = { MAP_TYPE_NORMAL, MAP_TYPE_HYBRID };

    private Runnable mapToggleView = new Runnable() {
        @Override
        public void run() {
            mapViewCounter++;
            mapViewCounter %= mapViews.length;
            mMap.setMapType(mapViews[mapViewCounter]);
        }
    };

    /**
     * The map. It is initialized when the map has been fully loaded and is ready to be used.
     *
     * @see #onMapReady(com.google.android.gms.maps.GoogleMap)
     */
    private GoogleMap mMap;

    public void onCreate(Bundle savedState) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS_REQUEST_RECORD_AUDIO);
        }

        super.onCreate(savedState);

        // Set the layout. It only contains a MapFragment and a DismissOverlay.
        setContentView(R.layout.activity_maps);

        // Retrieve the containers for the root of the layout and the map. Margins will need to be
        // set on them to account for the system window insets.
        final FrameLayout topFrameLayout = (FrameLayout) findViewById(R.id.root_container);
        final FrameLayout mapFrameLayout = (FrameLayout) findViewById(R.id.map_container);

        // Set the system view insets on the containers when they become available.
        topFrameLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                // Call through to super implementation and apply insets
                insets = topFrameLayout.onApplyWindowInsets(insets);

                FrameLayout.LayoutParams params =
                        (FrameLayout.LayoutParams) mapFrameLayout.getLayoutParams();

                // Add Wearable insets to FrameLayout container holding map as margins
                params.setMargins(
                        insets.getSystemWindowInsetLeft(),
                        insets.getSystemWindowInsetTop(),
                        insets.getSystemWindowInsetRight(),
                        insets.getSystemWindowInsetBottom());
                mapFrameLayout.setLayoutParams(params);

                return insets;
            }
        });

        // Obtain the DismissOverlayView and display the introductory help text.
        mDismissOverlay = (DismissOverlayView) findViewById(R.id.dismiss_overlay);
        mDismissOverlay.setIntroText(R.string.intro_text);
        mDismissOverlay.showIntroIfNecessary();

        // Obtain the MapFragment and set the async listener to be notified when the map is ready.
        MapFragment mapFragment =
                (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        dispatcher = AudioService.getInstance(false).getAudioDispatcher();
        final int audioBufferSize = AudioService.getInstance(false).getBufferSize();
        InputStream inputStream = null;
        try {
//				Log.d(TAG, "" + assetManager.getLocales());
            inputStream = getAssets().open("arff/MapCase.arff");
        } catch (IOException e) {
            e.printStackTrace();
        }
        final TrainedRecognizer eventRecognizer = new TrainedRecognizer(inputStream, "MFCC", true);
        eventRecognizer.setEventRecognitionListener(new TrainedRecognizer.EventRecognitionListener() {
            @Override
            public void onEventRecognized(String result) {
                Log.v("lastEvent", result);
                if (result.equals("clockwise")) {
                    Log.v("event recog", "short blow result");
                    runOnUiThread(mapZoomIn);
                } else if (result.equals("counterclockwise")) {
                    Log.v("event recog", "double blow result");
                    runOnUiThread(mapZoomOut);
                } else if (result.equals("topcenter")) {
                    runOnUiThread(mapPanNorth);
                } else if (result.equals("bottomcenter")) {
                    runOnUiThread(mapPanSouth);
                } else if (result.equals("right")) {
                    runOnUiThread(mapPanEast);
                } else if (result.equals("left")) {
                    runOnUiThread(mapPanWest);
                } else if (result.equals("swipeleft")) {
                    runOnUiThread(mapToggleView);
                } else if (result.equals("swiperight")) {
                    runOnUiThread(mapReCenter);
                }
            }
        });
        AudioService.getInstance(false).startThread(new AudioProcessor() {
            @Override
            public boolean process(AudioEvent audioEvent) {
                eventRecognizer.recognizeAudioBuffer(audioEvent);
                return true;
            }

            @Override
            public void processingFinished() {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Map is ready to be used.
        mMap = googleMap;
        // Set the long click listener as a way to exit the map.
        mMap.setOnMapLongClickListener(this);
        // Add a marker in Sydney, Australia and move the camera.

        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
	}

	@Override
	public void onMapLongClick(LatLng latLng) {
		// Display the dismiss overlay with a button to exit this activity.
		mDismissOverlay.show();
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode)
        {
            case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                }
                else
                {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}
