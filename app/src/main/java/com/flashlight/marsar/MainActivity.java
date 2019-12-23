package com.flashlight.marsar;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArFragment arFragment;
    private Button planeButton;
    private Button skyButton;
    private Boolean isTracking = false;
    private Boolean isHitting = false;

    private float currentDegree = 0f;
    TextView azimuthView;
    Compass compass;
    PlanetPosition planetPosition = new PlanetPosition();

    String[] planets = new String[] {"Mars", "Moon", "Venus", "Jupiter"};
    String[] planetObjects = new String[] {"scene.sfb", "Moon_1_3474.sfb"};
    int currentPlanet = 0;

    private DrawerLayout dl;
    private NavigationView nv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.sceneform_fragment);
        azimuthView = (TextView) findViewById(R.id.azimuth);
        setupCompass();

        // Добавляем листенер, вызывается перед каждым фреймом
        arFragment.getArSceneView().getScene().addOnUpdateListener( frameTime -> {
            arFragment.onUpdate(frameTime);
            this.onUpdate();
        });

        arFragment.getArSceneView().getScene().getCamera().setFarClipPlane(40);

        planeButton = (Button) findViewById(R.id.buttonAddToPlane);
        skyButton = (Button) findViewById(R.id.buttonUpdateAir);
        planeButton.setOnClickListener( (i) -> {
            this.addObject(Uri.parse(planetObjects[currentPlanet]));
        });
        skyButton.setOnClickListener(i -> {
            planetPosition.getPlanetPosition(planets[currentPlanet], this);
        });
        this.showFab(false);

        dl = (DrawerLayout) findViewById(R.id.content);
        // шторка
        nv = (NavigationView) findViewById(R.id.nv);
        nv.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();
                switch(id)
                {
                    case R.id.marsOption:
                        currentPlanet = 0;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView bigPlanet = (ImageView) findViewById(R.id.currentPlanetView);
                                bigPlanet.setImageResource(R.drawable.mars);
                                ((TextView) findViewById(R.id.currentPlanetName)).setText("Mars");
                            }
                        });
                        break;
                    case R.id.moonOption:
                        currentPlanet = 1;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageView bigPlanet = (ImageView) findViewById(R.id.currentPlanetView);
                                bigPlanet.setImageResource(R.drawable.moon);
                                ((TextView) findViewById(R.id.currentPlanetName)).setText("Moon");
                            }
                        });
                        break;
                    default:
                        return true;
                }
                return true;
            }
        });
    }

    private void showFab(Boolean enabled) {
        planeButton.setEnabled(enabled);
        if (!enabled) {
            planeButton.getBackground().setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);
        } else {
            planeButton.getBackground().setColorFilter(null);
        }
    }

    private void onUpdate() {
        updateTracking();
        if (isTracking) {
            Boolean hitTestChanged = updateHitTest();
            if (hitTestChanged) showFab(isHitting);
        }
    }

    private Boolean updateHitTest() {
      Frame frame = arFragment.getArSceneView().getArFrame();
      Point point = getScreenCenter();
      List<HitResult> hits;
      Boolean wasHitting = isHitting;
      isHitting = false;
      if (frame != null) {
          hits = frame.hitTest((float) point.x, (float) point.y);
          for (HitResult hit : hits) {
            Trackable trackable = hit.getTrackable();
            if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                isHitting = true;
                break;
            }
          }
      }
      return wasHitting != isHitting;
    }

    private Boolean updateTracking() {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Boolean wasTracking = isTracking;
        isTracking = frame.getCamera().getTrackingState() == TrackingState.TRACKING;
        return isTracking != wasTracking;
    }

    private Point getScreenCenter() {
        View view = findViewById(R.id.content);
        return new Point(view.getWidth() / 2, view.getHeight() / 2);
    }

    private void addObject(Uri model) {
        Frame frame = arFragment.getArSceneView().getArFrame();
        Point point = getScreenCenter();
        List<HitResult> hits;
        if (frame != null) {
            System.out.println(frame.getCamera().getDisplayOrientedPose().toString());
            hits = frame.hitTest((float) point.x, (float) point.y);
            for (HitResult hit : hits) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    placeObject(arFragment, hit.createAnchor(), model);
                    break;
                }
            }
        }
    }

    public void placeObject(ArFragment fragment, Anchor anchor, Uri model) {
        ModelRenderable.builder().setSource(fragment.getContext(), model).build().thenAccept((it) -> {
            addNodeToScene(fragment, anchor, it);
        }).exceptionally(e -> {
            Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
            System.out.println(e.toString());
            return null;
        });
    }

    private void addNodeToScene(ArFragment fragment, Anchor anchor, ModelRenderable renderable) {
        AnchorNode anchorNode = new AnchorNode(anchor);
        TransformableNode transformableNode = new TransformableNode(fragment.getTransformationSystem());
        transformableNode.setRenderable(renderable);
        transformableNode.setParent(anchorNode);
        fragment.getArSceneView().getScene().addChild(anchorNode);
        transformableNode.select();
    }

    public void setPlanetPosition(HashMap<String, Float> data) {
        System.out.println(data.toString());
        Uri model = Uri.parse(planetObjects[currentPlanet]);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                skyButton.setEnabled(true);
                skyButton.getBackground().setColorFilter(null);
                ModelRenderable.builder().setSource(arFragment.getContext(), model).build().thenAccept((it) -> {

                    Frame frame = arFragment.getArSceneView().getArFrame();
                    Pose p = frame.getCamera().getPose().compose(Pose.makeTranslation(0, 0, -5f)).extractTranslation();
                    Pose pwv = frame.getCamera().getPose();
                    Vector3 worldVector = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
                    worldVector.set(pwv.tx(), pwv.ty(), worldVector.z);


                    float distance = 5f;
                    float cos = (float) Math.cos(Math.toRadians(data.get("altitude").doubleValue()));
                    float cat = distance / cos;
                    float height = (float) Math.sqrt(cat*cat + distance*distance - 2 * distance * cat * cos);
                    if (data.get("altitude").doubleValue() < 0) height *= -1;

                    float cameraAngle = frame.getCamera().getDisplayOrientedPose().qy();
                    float delta = data.get("azimuth") - currentDegree;
                    float xOffset = (float) Math.sqrt(2 * distance * distance - 2 * distance * distance * Math.cos(delta));
                    if (delta < 0) xOffset *= -1;


                    //float height = (float) ((cos * Math.sqrt(1 - cos * cos)) / 5f);
                    //Toast.makeText(getApplicationContext(), "Alt: " + data.get("altitude") + ", Az: " + data.get("azimuth"), Toast.LENGTH_LONG).show();
                    System.out.println(delta + " Azimuth: " + data.get("azimuth") + " CurrentAz: " + currentDegree + " offset: " + xOffset + " cameraAngle:" + cameraAngle * 360);
                    //System.out.println(pwv.x + " " + pwv.y + " " + pwv.z);
                    Pose worldPose = Pose.makeTranslation(worldVector.x, 0, worldVector.z);
                    Pose displayPose = frame.getCamera().getDisplayOrientedPose();
                    Pose pw = worldPose.compose(Pose.makeTranslation(xOffset + displayPose.tx(), height, -5f));

                    Anchor anchor = arFragment.getArSceneView().getSession().createAnchor(pw);
                    AnchorNode anchorNode = new AnchorNode(anchor);

                    TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
                    transformableNode.setRenderable(it);
                    transformableNode.setParent(anchorNode);
                    arFragment.getArSceneView().getScene().addChild(anchorNode);
                    transformableNode.select();

                }).exceptionally(e -> {
                    Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_SHORT).show();
                    System.out.println(e.toString());
                    return null;
                });
            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        compass.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        compass.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Frame frame = arFragment.getArSceneView().getArFrame();
                        if (frame != null) {
                            //System.out.println(frame.getCamera().getDisplayOrientedPose().qy());
                        }
                        currentDegree = azimuth;
                        String t = Math.round(azimuth) + "°";
                        azimuthView.setText(t);
                    }
                });
            }
        };
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);

            return false;
        } else {
            return true;
        }
    }


}
