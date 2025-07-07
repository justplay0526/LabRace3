package com.example.lab10.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class FavoriteSpot {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String address;
    public double lat;
    public double lng;

    public FavoriteSpot(String name, String address, double lat, double lng) {
        this.name = name;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }
}
