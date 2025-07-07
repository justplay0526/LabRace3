package com.example.lab10.data;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {FavoriteSpot.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FavoriteDao favoriteDao();
}
