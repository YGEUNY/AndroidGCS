package com.example.ygygcs;

import android.graphics.PointF;
import android.support.annotation.NonNull;
import android.util.Log;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.LatLngBounds;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PolygonOverlay;
import com.o3dr.android.client.Drone;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.util.MathUtils;

import org.droidplanner.services.android.impl.core.helpers.geoTools.LineLatLong;
import org.droidplanner.services.android.impl.core.polygon.Polygon;
import org.droidplanner.services.android.impl.core.survey.grid.CircumscribedGrid;
import org.droidplanner.services.android.impl.core.survey.grid.Trimmer;

import java.util.ArrayList;
import java.util.List;

public class PolygonMission {
    private ArrayList<LatLong> polygonPointList = new ArrayList<>();
    private ArrayList<LatLong> sprayPotintList = new ArrayList<>();
    private List<Marker> targetMarker = new ArrayList<>();
    private Marker marker = new Marker();
    private PolygonOverlay polygon = new PolygonOverlay();

    protected double sprayAngle;
    private MainActivity mainActivity;

    private PolygonSprayState polygonSprayState;
    private LatLong pointA = null;
    private LatLong pointB = null;
    private double sprayDistance = 5.5f;
    private int maxSprayDistance = 50;
    private int capacity = 0, markerCount = 0;

    public static enum PolygonSprayState{
        NONE,
        STARTED,
        STORED_A,
        MARKED_SPRAYPOINT,
        UPLOADED_MISSION,
        PLAYING_MISSION,
        PAUSE_MISSION,
        FINISH_MISSION
    }

    public void setCapacity(int capacity){
        this.capacity = capacity;
    }

    PolygonMission(MainActivity activity){
        this.mainActivity = activity;
    }

    public void createPolygonPoint(LatLong latLong){
        double angle1 = 0, angle2 = 0;

        if(polygonSprayState == PolygonSprayState.NONE){
            polygonSprayState = PolygonSprayState.STARTED;
        }
        polygonPointList.add(latLong);

       //if(){
            if(polygonPointList.size() == 1){
                polygonSprayState = PolygonSprayState.STORED_A;
            }
            if(polygonPointList.size() == 2) {
                angle1 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(0), polygonPointList.get(1));
                LatLong newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(1), angle1, 100);
                createPolygonPoint(newPoint);

                angle1 = MathUtils.getHeadingFromCoordinates(polygonPointList.get(1), polygonPointList.get(0));
                newPoint = MathUtils.newCoordFromBearingAndDistance(polygonPointList.get(1), angle2, 100);
                createPolygonPoint(newPoint);
                polygonSprayState = PolygonSprayState.MARKED_SPRAYPOINT;
            }
       // }

        if(polygonPointList.size() >2){
//            drawABPolygon();
         //   if(){
//                sprayAngle = angle1;
//            }else{
//                sprayAngle = makeSprayAngle();
//            }

            try {
                makeGrid();
                polygonSprayState = PolygonSprayState.MARKED_SPRAYPOINT;
            }catch (Exception e){
                Log.d("myLog","예외처리 : " + e.getMessage());
            }
        }

    }

    public void modifyPolygonPoint(){
        if(polygonPointList.size() > 2){
         //   drawABPolygon();
            sprayAngle = makeSprayAngle();
            try {
                makeGrid();
                polygonSprayState = PolygonSprayState.MARKED_SPRAYPOINT;
            }catch (Exception e){
                Log.d("myLog","예외처리 : " + e.getMessage());
            }
        }
    }

    public void rotatiePath(double rotateAmount){

    }

    public void makeGrid() throws Exception {
        if(mainActivity == null) throw new Exception("PolygonSpray retreiving MapActivity returns null");

        List<LatLong> polygonPoints = new ArrayList<>();
        for(LatLong latLong : polygonPointList){
            polygonPoints.add(latLong);
        }
        List<LineLatLong> circumscribedGrid = new CircumscribedGrid(polygonPoints, this.sprayAngle, sprayDistance).getGrid();
        List<LineLatLong> trimedGrid  = new Trimmer(circumscribedGrid, makePoly().getLines()).getTrimmedGrid();

        for(int i = 0; i<trimedGrid.size(); i++){
            LineLatLong line = trimedGrid.get(i);
            if(line.getStart().getLatitude() > line.getEnd().getLatitude()){
                LineLatLong line1 = new LineLatLong(line.getEnd(), line.getStart());
                trimedGrid.set(i, line1);
            }
        }

//        LatLong dronePosition = mainActivity.this.drone.getManageDroneState().getDronePosition();
//        double dist1 = MathUtils.pointToLineDistance(trimedGrid.get(0));
    }

    public void drawABPolygon(Drone drone, final NaverMap myMap, double abDistance, double flightRange){

        myMap.setOnMapClickListener(new NaverMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull PointF pointF, @NonNull LatLng latLng) {
                markerCount++;
                if(markerCount<=2){
                    marker.setPosition(new LatLng(latLng.latitude, latLng.longitude));
                    targetMarker.add(marker);
                    if(markerCount==1)
                        targetMarker.get(markerCount-1).setIcon(OverlayImage.fromResource(R.drawable.marker_a));
                    else if(markerCount==2)
                        targetMarker.get(markerCount-1).setIcon(OverlayImage.fromResource(R.drawable.marker_b));
                    targetMarker.get(markerCount-1).setAnchor(new PointF(0.5F, 0.9F));
                    targetMarker.get(markerCount-1).setMap(myMap);
                }
            }
        });
        if(markerCount == 2){
            makePath(abDistance, flightRange);
        }
    }

    private void makePath(double abDistance, double flightRange){
        LatLngBounds area = new LatLngBounds(targetMarker.get(0).getPosition(), targetMarker.get(1).getPosition());
        area.buffer(abDistance);

        MathUtils
        abDistance/flightRange;
    }

    public double makeSprayAngle(){
        Polygon poly = makePoly();
        double angle = 0;
        double maxDistance = 0;
        List<LineLatLong> lineLatLongList = poly.getLines();
        for (LineLatLong lineLatLong : lineLatLongList) {
            double lineDistance = MathUtils.getDistance2D(lineLatLong.getStart(), lineLatLong.getEnd());
            if(maxDistance < lineDistance) {
                maxDistance = lineDistance;
                angle = lineLatLong.getHeading();
            }
        }
        return angle;
    }

    private Polygon makePoly(){
        Polygon poly = new Polygon();
        List<LatLong> latLongList = new ArrayList<>();
        for(LatLong latLong : polygonPointList) {
            latLongList.add(latLong);
        }
        poly.addPoints(latLongList);
        return poly;
    }
}
