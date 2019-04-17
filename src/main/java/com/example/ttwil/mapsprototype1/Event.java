package com.example.ttwil.mapsprototype1;

import com.google.android.gms.maps.model.LatLng;

public class Event {
    // VARIABLES
    private LatLng position;
    private String code;
    private String description;

    // DEFAULT CONSTRUCTOR
    public Event() {
        position = new LatLng(0,0);
        code = "";
        description = "";
    }

    // CONSTRUCTOR
    public Event(LatLng position, String code, String description) {
        this.position = position;
        this.code = code;
        this.description = description;
    }

    // CONSTRUCTOR
    public Event(LatLng position) {
        this.position = position;
        this.code = "";
        this.description = "";
    }

    // GETTERS AND SETTERS
    public LatLng getPosition() {
        return position;
    }

    public void setPosition(LatLng position) {
        this.position = position;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
