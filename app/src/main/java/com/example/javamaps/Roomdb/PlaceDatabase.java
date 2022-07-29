package com.example.javamaps.Roomdb;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.example.javamaps.Place.Place;

@Database(entities = {Place.class}, version = 1)
public abstract class PlaceDatabase extends RoomDatabase{
    public abstract PlaceDao placeDao();
}