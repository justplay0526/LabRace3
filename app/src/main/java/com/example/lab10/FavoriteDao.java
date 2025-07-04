package com.example.lab10;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert
    void insert(FavoriteSpot spot);

    @Query("SELECT * FROM FavoriteSpot WHERE name = :name AND lat = :lat AND lng = :lng LIMIT 1")
    FavoriteSpot findByNameAndLatLng(String name, double lat, double lng);

    @Query("SELECT * FROM FavoriteSpot")
    List<FavoriteSpot> getAll();

    @Delete
    void delete(FavoriteSpot spot);
}

