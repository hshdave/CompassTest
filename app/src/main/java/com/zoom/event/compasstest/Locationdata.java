package com.zoom.event.compasstest;

import android.os.Parcel;
import android.os.Parcelable;

public class Locationdata implements Parcelable{

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

    @Override
    public int describeContents() {
        return 0;
    }

    public Locationdata(Parcel in)
    {
        this.latitude = in.readDouble();
        this.longitude = in.readDouble();
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeDouble(latitude);
        dest.writeDouble(longitude);

    }

    public static final Parcelable.Creator<Locationdata> CREATOR = new Parcelable.Creator<Locationdata>() {

        public Locationdata createFromParcel(Parcel in) {
            return new Locationdata(in);
        }

        public Locationdata[] newArray(int size) {
            return new Locationdata[size];
        }
    };
}
