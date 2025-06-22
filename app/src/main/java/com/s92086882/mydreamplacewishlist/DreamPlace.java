package com.s92086882.mydreamplacewishlist;

import java.io.Serializable;
import java.util.List;

public class DreamPlace implements Serializable {
    private String id; // Firestore document ID
    private String name;
    private String city;
    private String notes;
    private List<String> photoPaths;
    private boolean visited;
    private float rating;
    private double latitude;
    private double longitude;
    private String distance;
    private double lat;
    private double lng;

    // Full Constructor
    public DreamPlace(List<String> photoPaths, String name, String city, String distance,
                      boolean visited, float rating, double latitude, double longitude, String notes) {
        this.name = name;
        this.city = city;
        this.notes = notes;
        this.photoPaths = photoPaths;
        this.visited = visited;
        this.rating = rating;
        this.latitude = latitude;
        this.longitude = longitude;
        this.distance = distance;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getNotes() { return notes; }
    public List<String> getPhotoPaths() { return photoPaths; }
    public boolean isVisited() { return visited; }
    public float getRating() { return rating; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getDistance() { return distance; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setCity(String city) { this.city = city; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setPhotoPaths(List<String> photoPaths) { this.photoPaths = photoPaths; }
    public void setVisited(boolean visited) { this.visited = visited; }
    public void setRating(float rating) { this.rating = rating; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setDistance(String distance) { this.distance = distance; }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }
}