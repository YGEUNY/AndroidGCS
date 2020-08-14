package com.example.ygygcs;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.overlay.Marker;
import com.o3dr.services.android.lib.coordinate.LatLong;

import java.util.ArrayList;
import java.util.List;

public class PolygonMission {
    private ArrayList<LatLong> polygonPointList = new ArrayList<>();
    private ArrayList<LatLong> sprayPotintList = new ArrayList<>();

    private MainActivity mainActivity;

    private LatLong pointA = null;
    private LatLong pointB = null;
    private double sprayDistance = 5.5f;
    private int maxSprayDistance = 50;
    private int capacity = 0;

    public static enum PolygonSprayState{
        NONE,
        STARTED,
        STORED_A,
        MARKED_SPRAYPoINT,
        UPLOADED_MISSION,
        PLAYING_MISSION,
        PAUSE_MISSION,
        FINISH_MISSION
    }

    public void setCapacity(int capacity){
        this.capacity = capacity;
    }




}
