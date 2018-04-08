package com.zoom.event.compasstest;

public class Locationdata {

    private Double latitude,longitude;


    public Locationdata(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }


    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
