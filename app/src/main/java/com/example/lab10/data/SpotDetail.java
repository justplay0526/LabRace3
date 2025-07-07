package com.example.lab10.data;

import java.io.Serializable;
import java.util.List;

public class SpotDetail implements Serializable {
    public String name;
    public String address;
    public String introduction;
    public List<String> imageUrls;

    public SpotDetail(String name, String address, String introduction, List<String> imageUrls) {
        this.name = name;
        this.address = address;
        this.introduction = introduction;
        this.imageUrls = imageUrls;
    }
}


