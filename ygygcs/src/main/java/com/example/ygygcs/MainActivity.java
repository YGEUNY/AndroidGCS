package com.example.ygygcs;

import android.content.Context;
import android.location.GpsSatellite;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;;

import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.ExperimentalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.android.client.utils.video.DecoderListener;
import com.o3dr.android.client.utils.video.MediaCodecManager;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.coordinate.LatLongAlt;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.mission.item.command.YawCondition;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.Home;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.o3dr.android.client.apis.ExperimentalApi.getApi;
public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private ControlTower controlTower;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();
    private MediaCodecManager mediaCodecManager;
    private int markerCount = 0;
    NaverMap myMap;
    final UiSettings uiSettings = myMap.getUiSettings();
    Button btnArm, btnFlightMode, btnMap, btnMapType, btnCadastral, btnClear, btnBasic, btnSatellite,  btnTerrain, btnDroneConnect;
    TableLayout visBtn;
    Boolean mapONOFf, mapClear, mapCadstral;
    Handler mainHandler;
    LatLng vehicleLatLng;
    List<Marker> markers = new ArrayList<>();
  //  Marker marker = new Marker();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
        btnStart();

    }

    public void btnStart(){
        btnArm = findViewById(R.id.arm);
        btnMap = findViewById(R.id.mapOnOff);
        btnMapType = findViewById(R.id.mapType);
        btnCadastral = findViewById(R.id.cadastralONOff);
        btnClear = findViewById(R.id.clear);
        btnBasic = findViewById(R.id.BasicMap);
        btnSatellite = findViewById(R.id.SatelliteMap);
        btnTerrain = findViewById(R.id.TerrainMap);
        visBtn = findViewById(R.id.buttonLayout);
        btnDroneConnect = findViewById(R.id.droneConnect);
        btnFlightMode = findViewById(R.id.flightMode);

        btnMapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(visBtn.getVisibility() == View.VISIBLE)
                    visBtn.setVisibility(View.GONE);
                else
                    visBtn.setVisibility(View.VISIBLE);
            }
        });

        btnBasic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Basic);
                btnMapType.setText("일반지도");
                visBtn.setVisibility(View.GONE);
            }
        });

        btnSatellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Satellite);
                btnMapType.setText("위성지도");
                visBtn.setVisibility(View.GONE);
            }
        });

        btnTerrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Terrain);
                btnMapType.setText("지형도");
                visBtn.setVisibility(View.GONE);
            }
        });

        mapCadstral = true;
        btnCadastral.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapCadstral){
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    btnCadastral.setText("지적도ON");
                    mapCadstral = false;
                }else{
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    btnCadastral.setText("지적도OFF");
                    mapCadstral = true;
                }
            }
        });

        mapONOFf = true;
        btnMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mapONOFf){
                    uiSettings.setScrollGesturesEnabled(false);
                    btnMap.setText("맵 해제");
                    mapONOFf = false;
                }else{
                    uiSettings.setScrollGesturesEnabled(true);
                    btnMap.setText("맵 잠금");
                    mapONOFf = true;
                }
            }
        });

        mapClear = true;
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (Marker_Count - 1 >= 0) {
//                    markers.get(Marker_Count - 1).setMap(null);
//                }

                if(mapClear){

                    mapClear = false;
                }else{

                    mapClear = true;
                }
            }
        });

        btnDroneConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                droneConnect();
            }
        });

        btnFlightMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        btnArm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                armConnect();
            }
        });
    }


    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;
        myMap.setMapType(NaverMap.MapType.Basic);


    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
        updateVehicleModesForType(this.droneType);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            updateConnectedButton(false);
        }

        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onTowerDisconnected() {
        alertUser("DroneKit-Android Interrupted");
    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                alertUser("Drone Connected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                updateFlightModeButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                updateFlightModeButton();
                clearInformation();
                break;

            case AttributeEvent.STATE_UPDATED:
            case AttributeEvent.STATE_ARMING:
                updateArmButton();
                break;

            case AttributeEvent.TYPE_UPDATED:
                Type newDroneType = this.drone.getAttribute(AttributeType.TYPE);
                if (newDroneType.getDroneType() != this.droneType) {
                    this.droneType = newDroneType.getDroneType();
                    updateVehicleModesForType(this.droneType);
                }
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                updateVehicleMode();
                break;

            case AttributeEvent.BATTERY_UPDATED:
                updateVolt();
                break;

            case AttributeEvent.SPEED_UPDATED:
                updateSpeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                updateAltitude();
                break;

            case AttributeEvent.GPS_COUNT:
                updateSatellite();
                break;

            case AttributeEvent.ATTITUDE_UPDATED:
                updateYaw();
                break;

            case AttributeEvent.GPS_POSITION:
                updateGPS();
                break;

//            case AttributeEvent.HOME_UPDATED:
//                updateDistanceFromHome();
//                break;

            default:
                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null){
            alertUser("Unable to retrieve the solo state.");
        }
        else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    public void droneConnect(){
        if(this.drone.isConnected()){
            this.drone.disconnect();
        }else{
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    public void armConnect(){
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);

        if (vehicleState.isFlying()) {
            // Land
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LAND, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to land the vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to land the vehicle.");
                }
            });
        } else if (vehicleState.isArmed()) {
            // Take off
            ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {

                @Override
                public void onSuccess() {
                    alertUser("Taking off...");
                }

                @Override
                public void onError(int i) {
                    alertUser("Unable to take off.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Unable to take off TimeOut");
                }
            });
        } else if (!vehicleState.isConnected()) {
            // Connect
            alertUser("Connect to a drone first");
        } else {
            // Connected but not Armed
            VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {
                @Override
                public void onError(int executionError) {
                    alertUser("Unable to arm vehicle.");
                }

                @Override
                public void onTimeout() {
                    alertUser("Arming operation timed out.");
                }
            });
        }
    }

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch(connectionStatus.getStatusCode()){
            case LinkConnectionStatus.FAILED:
                Bundle extras = connectionStatus.getExtras();
                String msg = null;
                if (extras != null) {
                    msg = extras.getString(LinkConnectionStatus.EXTRA_ERROR_MSG);
                }
                alertUser("Connection Failed:" + msg);
                break;
        }
    }

    @Override
    public void onTowerConnected() {
        alertUser("DroneKit-Android Connected");
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);
    }

    protected void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }

    protected void updateVolt(){
        TextView voltTextView = (TextView)findViewById(R.id.voltValueTextView);
        Battery droneVolt = this.drone.getAttribute(AttributeType.BATTERY);
        voltTextView.setText(String.format("%3.1f",droneVolt.getBatteryVoltage()) + "V");
    }

//    public void onFlightModeSelected(View view) {
//        VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
//
//        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
//            @Override
//            public void onSuccess() {
//                alertUser("Vehicle mode change successful.");
//            }
//
//            @Override
//            public void onError(int executionError) {
//                alertUser("Vehicle mode change failed: " + executionError);
//            }
//
//            @Override
//            public void onTimeout() {
//                alertUser("Vehicle mode change timed out.");
//            }
//        });
//    }

    protected void updateSpeed(){
        TextView speedTextView = (TextView)findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    protected void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
        }

    protected void updateYaw(){
        TextView yawTextView = (TextView)findViewById(R.id.yawValueTextView);
        Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if(droneYaw.getYaw() < 0){
            yawTextView.setText(String.format("%3.0f",(360 + droneYaw.getYaw())) + "deg");
        }else{
            yawTextView.setText(String.format("%3.0f",droneYaw.getYaw()) + "deg");
        }
    }

    protected void updateSatellite(){
        TextView satelliteTextView = (TextView)findViewById(R.id.satelliteValueTextView);
        Gps droneSatellite = this.drone.getAttribute(AttributeType.GPS);
        satelliteTextView.setText(droneSatellite.getSatellitesCount() + "개");
    }

    protected void updateGPS(){
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        if(vehiclePosition == null) {
            //   Toast.makeText(getApplicationContext(), "GPS값 NULL", Toast.LENGTH_SHORT).show();
            Log.d("Gps값 null","다시");
          //  Toast.makeText(getApplicationContext(), "다시", Toast.LENGTH_SHORT).show();
        }else{
            try {
                vehicleLatLng = new LatLng(vehiclePosition.getLatitude(), vehiclePosition.getLongitude());
            } catch (Exception e) {
                Log.d("errorCheckLog", e.getLocalizedMessage().toString());
            }

        //    Toast.makeText(getApplicationContext(), "위도 : " + vehiclePosition.getLatitude() + "  /  경도 : " + vehiclePosition.getLongitude(), Toast.LENGTH_SHORT).show();
         Log.d("GPS값", "위도 : " + vehiclePosition.getLatitude() + "  /  경도 : " + vehiclePosition.getLongitude());
         CameraUpdate cameraUpdate = CameraUpdate.scrollTo(vehicleLatLng);
         myMap.moveCamera(cameraUpdate);
         if(markerCount - 1 >= 0){
             markers.get(markerCount - 1).setMap(null);
         }
         markers.add(new Marker(vehicleLatLng));
         //setPosition(vehicleLatLng);
         //marker.setMap(myMap);
         Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
         double yaw = droneYaw.getYaw();
         if((int) yaw < 0){
             yaw += 360;
         }

         markers.get(markerCount).setIcon(OverlayImage.fromResource(R.drawable.));
       }
    }

    protected void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.droneConnect);
        if (isConnected) {
            connectButton.setText("연결끊기");
        } else {
            connectButton.setText("연결하기");
        }
    }

    protected void updateArmButton() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        btnArm = (Button) findViewById(R.id.arm);
        //Button armButton
        if (!this.drone.isConnected()) {
            btnArm.setVisibility(View.INVISIBLE);
        } else {
            btnArm.setVisibility(View.VISIBLE);
        }

        if (vehicleState.isFlying()) {
            // Land
            btnArm.setText("LAND");
        } else if (vehicleState.isArmed()) {
            // Take off
            btnArm.setText("TAKE OFF");
        } else if (vehicleState.isConnected()) {
            // Connected but not Armed
            btnArm.setText("ARM");
        }
    }

    protected void updateFlightModeButton(){
        btnFlightMode = findViewById(R.id.flightMode);
        if (!this.drone.isConnected()) {
            btnFlightMode.setVisibility(View.INVISIBLE);
        } else {
            btnFlightMode.setVisibility(View.VISIBLE);
        }
    }

    protected void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
   //     this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    protected void updateVehicleMode() {
        TextView modeTextView = (TextView)findViewById(R.id.vehicleModeValueTextView);
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        modeTextView.setText(vehicleMode.getMode());
//        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
//        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
    }

    protected void clearInformation(){
        TextView voltTextView = (TextView) findViewById(R.id.voltValueTextView);
        voltTextView.setText("");

        TextView flightModeTextView = (TextView) findViewById(R.id.vehicleModeValueTextView);
        flightModeTextView.setText("");

        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        altitudeTextView.setText("");

        TextView speedTextView = (TextView)findViewById(R.id.speedValueTextView);
        speedTextView.setText("");

        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        yawTextView.setText("");

        TextView satelliteTextView = (TextView) findViewById(R.id.satelliteValueTextView);
        satelliteTextView.setText("");
    }

    protected double distanceBetweenPoints(LatLongAlt pointA, LatLongAlt pointB) {
        if (pointA == null || pointB == null) {
            return 0;
        }
        double dx = pointA.getLatitude() - pointB.getLatitude();
        double dy = pointA.getLongitude() - pointB.getLongitude();
        double dz = pointA.getAltitude() - pointB.getAltitude();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
