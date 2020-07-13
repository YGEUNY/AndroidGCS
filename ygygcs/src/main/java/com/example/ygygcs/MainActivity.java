package com.example.ygygcs;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;

import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    NaverMap myMap;
    Button btnArm, btnTake, btnLand, btnMap, btnMapType, btnCadastral, btnClear, btnBasic, btnSatellite,  btnTerrain;
    TableLayout visBtn;
    Boolean mapONOFf, mapClear, mapCadstral;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        btnTake = findViewById(R.id.takeOff);
        btnLand = findViewById(R.id.land);
        btnMap = findViewById(R.id.mapOnOff);
        btnMapType = findViewById(R.id.mapType);
        btnCadastral = findViewById(R.id.cadastralONOff);
        btnClear = findViewById(R.id.clear);
        btnBasic = findViewById(R.id.BasicMap);
        btnSatellite = findViewById(R.id.SatelliteMap);
        btnTerrain = findViewById(R.id.TerrainMap);
        visBtn = findViewById(R.id.buttonLayout);

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
            }
        });

        btnSatellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Satellite);
                btnMapType.setText("위성지도");
            }
        });

        btnTerrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMap.setMapType(NaverMap.MapType.Terrain);
                btnMapType.setText("지형도");
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

                }else{
                    btnMap.setText("맵 잠금");
                    mapONOFf = true;
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.myMap = naverMap;
        //   MyAsyncTask.execute();
        myMap.setMapType(NaverMap.MapType.Basic);
    }
}
