package com.s92086882.mydreamplacewishlist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DreamPlaceSQLiteHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "dream_places.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "places";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CITY = "city";
    public static final String COLUMN_NOTES = "notes";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LNG = "longitude";
    public static final String COLUMN_PHOTOS = "photos"; // Comma-separated URIs
    public static final String COLUMN_VISITED = "visited"; // 0 or 1
    public static final String COLUMN_RATING = "rating"; // float

    public DreamPlaceSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_NAME + " TEXT, " +
                COLUMN_CITY + " TEXT, " +
                COLUMN_NOTES + " TEXT, " +
                COLUMN_LAT + " REAL, " +
                COLUMN_LNG + " REAL, " +
                COLUMN_PHOTOS + " TEXT, " +
                COLUMN_VISITED + " INTEGER, " +
                COLUMN_RATING + " REAL)";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Save a new dream place
    public void addDreamPlace(DreamPlace place, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, place.getName());
        values.put(COLUMN_CITY, place.getCity());
        values.put(COLUMN_NOTES, place.getNotes());
        values.put(COLUMN_LAT, lat);
        values.put(COLUMN_LNG, lng);
        values.put(COLUMN_PHOTOS, String.join(",", place.getPhotoPaths()));
        values.put(COLUMN_VISITED, place.isVisited() ? 1 : 0);
        values.put(COLUMN_RATING, place.getRating());

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Load places and calculate distance, then return sorted list
    public List<DreamPlace> getAllPlacesOrderedByDistance(double currentLat, double currentLng) {
        List<DreamPlace> places = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String city = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LNG));
                String photosStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTOS));
                boolean visited = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VISITED)) == 1;
                float rating = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RATING));

                List<String> photoPaths = photosStr == null || photosStr.isEmpty()
                        ? new ArrayList<>()
                        : Arrays.asList(photosStr.split(","));

                double distance = calculateDistance(currentLat, currentLng, lat, lng);
                String distanceStr = String.format("%.1f km away", distance);

                places.add(new DreamPlace(photoPaths, name, city, distanceStr, visited, rating, lat, lng, notes));
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return places;
    }

    // Calculate distance between two coordinates in km
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0;
    }

    public void updateDreamPlace(DreamPlace place) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, place.getName());
        values.put(COLUMN_CITY, place.getCity());
        values.put(COLUMN_NOTES, place.getNotes());
        values.put(COLUMN_LAT, place.getLatitude());
        values.put(COLUMN_LNG, place.getLongitude());
        values.put(COLUMN_VISITED, place.isVisited() ? 1 : 0);
        values.put(COLUMN_RATING, place.getRating());
        values.put(COLUMN_PHOTOS, TextUtils.join(",", place.getPhotoPaths()));

        db.update(TABLE_NAME, values, COLUMN_NAME + "=?", new String[]{place.getName()});
        db.close();
    }
}