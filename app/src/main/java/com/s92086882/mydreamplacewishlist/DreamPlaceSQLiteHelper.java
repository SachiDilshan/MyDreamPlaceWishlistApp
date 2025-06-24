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

// SQLite helper class for managing guest user dream places
public class DreamPlaceSQLiteHelper extends SQLiteOpenHelper {

    // Database metadata
    private static final String DATABASE_NAME = "dream_places.db";
    private static final int DATABASE_VERSION = 1;

    // Table and column names
    public static final String TABLE_NAME = "places";
    public static final String COLUMN_ID = "id"; // Primary key
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_CITY = "city";
    public static final String COLUMN_NOTES = "notes";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LNG = "longitude";
    public static final String COLUMN_PHOTOS = "photos"; // Comma-separated list
    public static final String COLUMN_VISITED = "visited"; // 0 = false, 1 = true
    public static final String COLUMN_RATING = "rating"; // float value

    // Constructor
    public DreamPlaceSQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Create table schema
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

    // Handle schema upgrades
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    // Insert new dream place into database
    public void addDreamPlace(DreamPlace place, double lat, double lng) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_NAME, place.getName());
        values.put(COLUMN_CITY, place.getCity());
        values.put(COLUMN_NOTES, place.getNotes());
        values.put(COLUMN_LAT, lat);
        values.put(COLUMN_LNG, lng);
        values.put(COLUMN_PHOTOS, TextUtils.join(",", place.getPhotoPaths()));
        values.put(COLUMN_VISITED, place.isVisited() ? 1 : 0);
        values.put(COLUMN_RATING, place.getRating());

        db.insert(TABLE_NAME, null, values);
        db.close();
    }

    // Load all dream places and calculate distance from current location
    public List<DreamPlace> getAllPlacesOrderedByDistance(double currentLat, double currentLng) {
        List<DreamPlace> places = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // Read column values
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
                String city = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CITY));
                String notes = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NOTES));
                double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LAT));
                double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_LNG));
                String photosStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTOS));
                boolean visited = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VISITED)) == 1;
                float rating = cursor.getFloat(cursor.getColumnIndexOrThrow(COLUMN_RATING));

                // Convert comma-separated photo URIs to list
                List<String> photoPaths = photosStr == null || photosStr.isEmpty()
                        ? new ArrayList<>()
                        : Arrays.asList(photosStr.split(","));

                // Calculate distance from current location
                double distance = calculateDistance(currentLat, currentLng, lat, lng);
                String distanceStr = String.format("%.1f km away", distance);

                // Create DreamPlace and assign DB ID
                DreamPlace place = new DreamPlace(photoPaths, name, city, distanceStr, visited, rating, lat, lng, notes);
                place.setId(String.valueOf(id)); // Save SQLite row ID as string
                places.add(place);
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return places;
    }

    // Haversine distance calculation using Android API
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lon1, lat2, lon2, results);
        return results[0] / 1000.0; // Convert to kilometers
    }

    // Update an existing dream place using its unique database ID
    public void updateDreamPlace(DreamPlace place) {
        if (place.getId() == null) return;

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

        // Update by ID instead of name to ensure uniqueness
        db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{place.getId()});
        db.close();
    }

    // Optional helper to remove a photo from a place by ID and update DB
    public void deletePhotoFromPlace(String placeId, String photoToDelete) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, COLUMN_ID + "=?", new String[]{placeId}, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            String photosStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTOS));
            cursor.close();

            if (photosStr != null) {
                List<String> photos = new ArrayList<>(Arrays.asList(photosStr.split(",")));
                if (photos.remove(photoToDelete)) {
                    ContentValues values = new ContentValues();
                    values.put(COLUMN_PHOTOS, TextUtils.join(",", photos));
                    db.update(TABLE_NAME, values, COLUMN_ID + "=?", new String[]{placeId});
                }
            }
        }
        db.close();
    }

    // Delete a dream place permanently by ID (used for swipe-to-delete)
    public void deleteDreamPlaceById(String placeId) {
        if (placeId == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_ID + "=?", new String[]{placeId});
        db.close();
    }
    // Delete a dream place by its name (used during swipe-to-delete for guest users)
    public void deleteDreamPlaceByName(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_NAME + "=?", new String[]{name});
        db.close();
    }

    // Return all dream places without calculating distance (for use in Map view)
    public List<DreamPlace> getAllDreamPlaces() {
        List<DreamPlace> places = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
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

                // No need to calculate distance here
                DreamPlace place = new DreamPlace(photoPaths, name, city, "", visited, rating, lat, lng, notes);
                place.setId(String.valueOf(id));
                places.add(place);
            } while (cursor.moveToNext());
            cursor.close();
        }

        db.close();
        return places;
    }
}