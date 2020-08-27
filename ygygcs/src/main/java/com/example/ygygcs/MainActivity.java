package com.example.ygygcs;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;;

import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolylineOverlay;
import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.LinkListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Attitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.GuidedState;
import com.o3dr.services.android.lib.drone.property.Speed;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.gcs.link.LinkConnectionStatus;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, DroneListener, TowerListener, LinkListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private Drone drone;
    private ControlTower controlTower;
    private int droneType = Type.TYPE_UNKNOWN;
    private final Handler handler = new Handler();
    private NaverMap myMap;
    private double takeOffAltitude = 3.5, flightRange = 5, abDistance = 50;
    private Spinner modeSelector;
    private UiSettings uiSettings;

    private Button btnABDistance, btnABDistanceUp, btnABDistanceDown, btnMission, btnFlightRange, btnFlightRangeUp, btnFlightRangeDown, btnMissionAB, btnMissionPolygon, btnMissionCancel, btnArm, btnTakeOffAltitude, btnTakeOffUp, btnTakeOffDown, btnMapLock, btnMapType, btnCadastral, btnClear, btnBasic, btnSatellite, btnTerrain, btnDroneConnect;
    private TableLayout visBtn, visSpinner, missionLayout, flightRangeLayout, abDistanceLayout;
    private LinearLayout takeOffLayout;
    private Boolean mapONOFf, mapCadstral;
    private LatLng vehicleLatLng;
    private LatLng mGuidePoint;  //가이드모드 목적지 저장
    private Marker marker = new Marker();
    private Marker mMarkerGuide = new Marker();
    private PolylineOverlay polyline = new PolylineOverlay();
    private List<LatLng> coords = new ArrayList<>();
    private PolygonMission polygonMission;
    private MainActivity mainActivity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);
        polygonMission = new PolygonMission(mainActivity);

        this.modeSelector = (Spinner) findViewById(R.id.flightModeSelector);
        this.modeSelector.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                onFlightModeSelected(view);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //Do nothing
            }
        });

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;
        myMap.setMapType(NaverMap.MapType.Basic);

        uiSettings = naverMap.getUiSettings();
        uiSettings.setScrollGesturesEnabled(true);

        myMap.setOnMapLongClickListener(new NaverMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(@NonNull PointF pointF, @NonNull LatLng latLng) {

                mGuidePoint = new LatLng(latLng.latitude, latLng.longitude);
                if(drone.isConnected()){
                    startGuidedMode(latLng);
                }else{
                    alertUser("기체를 연결하세요");
                }

            }
        });

        btnStart();
    }

    // <<<<<<<<<<<<<===== 롱클릭 할 때 가이드모드 =====>>>>>>>>>>>>>>>
    private void startGuidedMode(@NonNull final LatLng latLng) {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        if(vehicleMode == VehicleMode.COPTER_GUIDED && mGuidePoint != null){
            alertUser("목적지를 변견합니다.");
           goToeGoal(latLng);
        }
        else{
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("확인하시면 가이드모드로 전환 후 기체가 이동합니다.");
            builder.setCancelable(false).setPositiveButton
                    ("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Action for 'Yes' Button
                            goToeGoal(latLng);
                        }
                    }).setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
        }
    }

    private void goToeGoal(@NonNull final LatLng latLng){
        mMarkerGuide.setPosition(new LatLng(latLng.latitude, latLng.longitude));
        mMarkerGuide.setMap(myMap);
        mMarkerGuide.setIcon(OverlayImage.fromResource(R.drawable.flag_icon));
        mMarkerGuide.setHeight(70);
        mMarkerGuide.setWidth(70);

        VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                ControlApi.getApi(drone).goTo(new LatLong(latLng.latitude, latLng.longitude), true, null);
                alertUser("목적지로 이동합니다.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("Guide모드 실패");
            }

            @Override
            public void onTimeout() {
                alertUser("Guide모드 실패");
            }
        });
    }

    private boolean checkGoal(LatLng recentLatLng) {
        GuidedState guidedState = this.drone.getAttribute(AttributeType.GUIDED_STATE);
        LatLng target = new LatLng(guidedState.getCoordinate().getLatitude(),
                guidedState.getCoordinate().getLongitude());
        return target.distanceTo(recentLatLng) <= 1;
    }

    private void changeToLoiter(){
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("Loiter 모드로 변경 중...");
            }

            @Override
            public void onError(int executionError) {
                alertUser("비행 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("비행 모드 변경 시간 초과.");
            }
        });
    }

    // 버튼 이벤트
    private void btnStart() {
        btnArm = findViewById(R.id.arm);
        btnMapLock = findViewById(R.id.mapLock);
        btnMapType = findViewById(R.id.mapType);
        btnCadastral = findViewById(R.id.cadastralONOff);
        btnClear = findViewById(R.id.clear);
        btnBasic = findViewById(R.id.BasicMap);
        btnSatellite = findViewById(R.id.SatelliteMap);
        btnTerrain = findViewById(R.id.TerrainMap);
        btnDroneConnect = findViewById(R.id.droneConnect);
        btnTakeOffAltitude = findViewById(R.id.takeOffBtn);
        btnTakeOffUp = findViewById(R.id.takeOffUp);
        btnTakeOffDown = findViewById(R.id.takeOffDown);
        btnMission = findViewById(R.id.mission);
        btnMissionAB = findViewById(R.id.missionAB);
        btnMissionPolygon = findViewById(R.id.missionPolygon);
        btnMissionCancel = findViewById(R.id.missionCancel);
        btnFlightRange = findViewById(R.id.flightRange);
        btnFlightRangeUp = findViewById(R.id.flightRangeUp);
        btnFlightRangeDown = findViewById(R.id.flightRangeDown);
        btnABDistance = findViewById(R.id.ABDistance);
        btnABDistanceUp = findViewById(R.id.ABDistanceUp);
        btnABDistanceDown = findViewById(R.id.ABDistanceDown);

        missionLayout = findViewById(R.id.missionLayout);
        flightRangeLayout = findViewById(R.id.flightRangeLayout);
        takeOffLayout = findViewById(R.id.takeOffLayout);
        visBtn = findViewById(R.id.buttonLayout);
        abDistanceLayout = findViewById(R.id.ABDistanceLayout);

        uiSettings = myMap.getUiSettings();

        btnMission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(missionLayout.getVisibility() == View.VISIBLE)
                    missionLayout.setVisibility(View.GONE);
                else
                    missionLayout.setVisibility(View.VISIBLE);
            }
        });

        btnMissionAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnMission.setText("AB");
                polygonMission.drawABPolygon(drone, myMap, abDistance, flightRange);
            }
        });

        btnMissionPolygon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createPolygonMarker();
            }
        });

        btnMissionCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        btnFlightRange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(flightRangeLayout.getVisibility() == View.VISIBLE)
                    flightRangeLayout.setVisibility(View.GONE);
                else
                    flightRangeLayout.setVisibility(View.VISIBLE);
            }
        });

        btnFlightRangeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flightRange += 0.5;
                btnFlightRange.setText(flightRange+"m\n비행폭");
            }
        });

        btnFlightRangeDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flightRange -= 0.5;
                btnFlightRange.setText(flightRange+"m\n비행폭");
            }
        });

        btnABDistance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(abDistanceLayout.getVisibility() == View.VISIBLE)
                    abDistanceLayout.setVisibility(View.GONE);
                else
                    abDistanceLayout.setVisibility(View.VISIBLE);
            }
        });

        btnABDistanceUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abDistance += 10;
                btnABDistance.setText(abDistance + "m\nAB거리");
            }
        });

        btnABDistanceDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abDistance -= 10;
                btnABDistance.setText(abDistance + "m\nAB거리");
            }
        });

        btnTakeOffAltitude.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (takeOffLayout.getVisibility() == View.VISIBLE)
                    takeOffLayout.setVisibility(View.GONE);
                else
                    takeOffLayout.setVisibility(View.VISIBLE);
            }
        });

        btnTakeOffUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeOffAltitude += 0.5;
                btnTakeOffAltitude.setText(takeOffAltitude + "m\n이륙고도");
            }
        });

        btnTakeOffDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takeOffAltitude -= 0.5;
                btnTakeOffAltitude.setText(takeOffAltitude + "m\n이륙고도");
            }
        });

        btnMapType.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (visBtn.getVisibility() == View.VISIBLE)
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
                if (mapCadstral) {
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true);
                    btnCadastral.setText("지적도OFF");
                    mapCadstral = false;
                } else {
                    myMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false);
                    btnCadastral.setText("지적도ON");
                    mapCadstral = true;
                }
            }
        });

        mapONOFf = true;
        btnMapLock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapONOFf) {
                    uiSettings.setScrollGesturesEnabled(false);
                    btnMapLock.setText("맵 해제하기");
                    mapONOFf = false;
                } else {
                    uiSettings.setScrollGesturesEnabled(true);
                    btnMapLock.setText("맵 잠금하기");
                    mapONOFf = true;
                }
            }
        });

        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                polyline.setMap(null);
                mMarkerGuide.setMap(null);

            }
        });

        btnDroneConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                droneConnect();
            }
        });

        btnArm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                armConnect();
            }
        });
    }

    // <<<<<<<<<<<<<<<<<< ====== 임무수행 이벤트 ====== >>>>>>>>>>>>>>>>>>>>>>>>
    private void createABMarker(){
        myMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
              // missionMarker.add(latLng, );
            }
        });
    }

    private void createPolygonMarker(){
        myMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {

            }
        });
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
                updateTakeOffButton();
                updateFlightModeButton();
                updateMissionButton();
                checkSoloState();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                alertUser("Drone Disconnected");
                updateConnectedButton(this.drone.isConnected());
                updateArmButton();
                updateTakeOffButton();
                updateFlightModeButton();
                updateMissionButton();
                clearInformation();
                mMarkerGuide.setMap(null);
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

            default:
                Log.i("DRONE_EVENT", event); //Uncomment to see events from the drone
                break;
        }
    }

    private void checkSoloState() {
        final SoloState soloState = drone.getAttribute(SoloAttributes.SOLO_STATE);
        if (soloState == null) {
            alertUser("Unable to retrieve the solo state.");
        } else {
            alertUser("Solo state is up to date.");
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {
    }

    // <<<<<<<<<<<<<<<<<<< ===== 드론연걸, 아밍 ===== >>>>>>>>>>>>>>>>>>>>>
    private void droneConnect() {
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //Log.e("mylog","버튼connect");
        } else {
            //   Log.e("mylog","버튼DIsconnect");
            ConnectionParameter connectionParams = ConnectionParameter.newUdpConnection(null);
            this.drone.connect(connectionParams);
        }
    }

    private void armConnect() {
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
            ControlApi.getApi(this.drone).takeoff(takeOffAltitude, new AbstractCommandListener() {

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
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Arming중");
            builder.setMessage("모터를 가동합니다\n모터가 고속으로 회전합니다.");
            builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    arming();
                }
            });
            builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.cancel();
                }
            });
            builder.show();
        }
    }

    private void arming() {
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

    @Override
    public void onLinkStateUpdated(@NonNull LinkConnectionStatus connectionStatus) {
        switch (connectionStatus.getStatusCode()) {
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

    private void alertUser(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        Log.d(TAG, message);
    }


    // <<<<<<<<<<<<<<<<< ====== 드론 이벤트 ===== >>>>>>>>>>>>>>>>>>>>>>>>
    private void updateVolt() {
        TextView voltTextView = (TextView) findViewById(R.id.voltValueTextView);
        Battery droneVolt = this.drone.getAttribute(AttributeType.BATTERY);
        voltTextView.setText(String.format("%3.1f", droneVolt.getBatteryVoltage()) + "V");
    }

    private void updateSpeed() {
        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        Speed droneSpeed = this.drone.getAttribute(AttributeType.SPEED);
        speedTextView.setText(String.format("%3.1f", droneSpeed.getGroundSpeed()) + "m/s");
    }

    private void updateAltitude() {
        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
        altitudeTextView.setText(String.format("%3.1f", droneAltitude.getAltitude()) + "m");
    }

    private void updateYaw() {
        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
        if (droneYaw.getYaw() < 0) {
            yawTextView.setText(String.format("%3.0f", (360 + droneYaw.getYaw())) + "deg");
        } else {
            yawTextView.setText(String.format("%3.0f", droneYaw.getYaw()) + "deg");
        }
    }

    private void updateSatellite() {
        TextView satelliteTextView = (TextView) findViewById(R.id.satelliteValueTextView);
        Gps droneSatellite = this.drone.getAttribute(AttributeType.GPS);
        satelliteTextView.setText(droneSatellite.getSatellitesCount() + "개");
    }

    private void updateGPS() {
        Gps droneGps = this.drone.getAttribute(AttributeType.GPS);
        LatLong vehiclePosition = droneGps.getPosition();

        if (vehiclePosition == null) {
            Log.d("Gps값 null", "다시");
        } else {
            try {
                vehicleLatLng = new LatLng(vehiclePosition.getLatitude(), vehiclePosition.getLongitude());
            } catch (Exception e) {
                Log.d("errorCheckLog", e.getLocalizedMessage().toString());
            }

            Log.d("GPS값", "위도 : " + vehiclePosition.getLatitude() + "  /  경도 : " + vehiclePosition.getLongitude());

            marker.setPosition(vehicleLatLng);
            Attitude droneYaw = this.drone.getAttribute(AttributeType.ATTITUDE);
                double yaw = droneYaw.getYaw();
                if ((int) yaw < 0) {
                    yaw += 360;
                }
                marker.setAngle((float) yaw);

                marker.setIcon(OverlayImage.fromResource(R.drawable.drone_marker));
                marker.setAnchor(new PointF(0.5F, 0.77F));
                marker.setMap(myMap);
                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(marker.getPosition());
                myMap.moveCamera(cameraUpdate);

                State vehicleState = this.drone.getAttribute(AttributeType.STATE);
                VehicleMode vehicleMode = vehicleState.getVehicleMode();
                if(vehicleMode == VehicleMode.COPTER_GUIDED && mGuidePoint != null){
                    if(checkGoal(mGuidePoint) == false) {
                        alertUser("목적지로 이동 성공");
                        changeToLoiter();
                        mMarkerGuide.setMap(null);
                        mGuidePoint = null;
                    }
                }

                Collections.addAll(coords, marker.getPosition());
                polyline.setCoords(coords);
                polyline.setColor(Color.GREEN);
                polyline.setMap(myMap);
                polyline.setWidth(10);
            }
    }

    // <<<<<<<<<<<<< ==== 드론 연결됐을 때 버튼 활성화 ===== >>>>>>>>>>>>>>>>>>>>>
    private void updateConnectedButton(Boolean isConnected) {
        Button connectButton = (Button) findViewById(R.id.droneConnect);
        if (isConnected) {
            connectButton.setText("연결끊기");
        } else {
            connectButton.setText("연결하기");
        }
    }

    private void updateArmButton() {
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

    private void updateFlightModeButton() {
        visSpinner = findViewById(R.id.spinnerLayout);
        if (!this.drone.isConnected()) {
            visSpinner.setVisibility(View.INVISIBLE);
        } else {
            visSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void updateTakeOffButton() {
        btnTakeOffAltitude = findViewById(R.id.takeOffBtn);
        if (!this.drone.isConnected())
            btnTakeOffAltitude.setVisibility(View.GONE);
        else {
            btnTakeOffAltitude.setText(takeOffAltitude + "m\n이륙고도");
            btnTakeOffAltitude.setVisibility(View.VISIBLE);
        }
    }

    private void updateMissionButton(){
        btnFlightRange = findViewById(R.id.flightRange);
        btnMission = findViewById(R.id.mission);
        btnABDistance = findViewById(R.id.ABDistance);
        if(this.drone.isConnected()){
            btnFlightRange.setVisibility(View.VISIBLE);
            btnMission.setVisibility(View.VISIBLE);
            btnABDistance.setVisibility(View.VISIBLE);
        }else{
            btnFlightRange.setVisibility(View.GONE);
            btnMission.setVisibility(View.GONE);
            btnABDistance.setVisibility(View.GONE);
        }
    }

    //  <<<<<<<<<<<<<< ===== 비행모드 세팅, 변경 ===== >>>>>>>>>>>>>>>>>>>>>
    private void onFlightModeSelected(View view) {
        final VehicleMode vehicleMode = (VehicleMode) this.modeSelector.getSelectedItem();
        VehicleApi.getApi(this.drone).setVehicleMode(vehicleMode, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                alertUser("비행 모드 " + vehicleMode.toString() + "로 변경 완료.");
            }

            @Override
            public void onError(int executionError) {
                alertUser("비행 모드 변경 실패 : " + executionError);
            }

            @Override
            public void onTimeout() {
                alertUser("비행 모드 변경 시간 초과.");
            }
        });
    }

    private void updateVehicleModesForType(int droneType) {
        List<VehicleMode> vehicleModes = VehicleMode.getVehicleModePerDroneType(droneType);
        ArrayAdapter<VehicleMode> vehicleModeArrayAdapter = new ArrayAdapter<VehicleMode>(this, android.R.layout.simple_spinner_item, vehicleModes);
        vehicleModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.modeSelector.setAdapter(vehicleModeArrayAdapter);
    }

    private void updateVehicleMode() {
        State vehicleState = this.drone.getAttribute(AttributeType.STATE);
        VehicleMode vehicleMode = vehicleState.getVehicleMode();
        ArrayAdapter arrayAdapter = (ArrayAdapter) this.modeSelector.getAdapter();
        this.modeSelector.setSelection(arrayAdapter.getPosition(vehicleMode));
        TextView flightModeTextView = (TextView) findViewById(R.id.vehicleModeValueTextView);
        flightModeTextView.setText(vehicleMode.toString());
    }

    // <<<<<<<<<<====== 정보창 리셋 ===== >>>>>>>>>>>>>
    private void clearInformation() {
        TextView voltTextView = (TextView) findViewById(R.id.voltValueTextView);
        voltTextView.setText("");

        TextView flightModeTextView = (TextView) findViewById(R.id.vehicleModeValueTextView);
        flightModeTextView.setText("");

        TextView altitudeTextView = (TextView) findViewById(R.id.altitudeValueTextView);
        altitudeTextView.setText("");

        TextView speedTextView = (TextView) findViewById(R.id.speedValueTextView);
        speedTextView.setText("");

        TextView yawTextView = (TextView) findViewById(R.id.yawValueTextView);
        yawTextView.setText("");

        TextView satelliteTextView = (TextView) findViewById(R.id.satelliteValueTextView);
        satelliteTextView.setText("");
    }
}
