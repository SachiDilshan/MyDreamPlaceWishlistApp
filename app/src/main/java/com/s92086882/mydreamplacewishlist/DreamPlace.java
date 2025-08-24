package com.s92086882.mydreamplacewishlist;

import java.io.Serializable;
import java.util.List;

/**
 * Model class representing a Dream Place entry.
 * -
 * Responsibilities:
 * - Holds all properties for a "dream place" (name, city, notes, photos, etc.).
 * - Works as a transfer object for both SQLite (guest users) and Firestore (logged-in users).
 * - Implements Serializable → allows passing DreamPlace objects through Intents/Bundle between Activities.
 */

public class DreamPlace implements Serializable {

    private String id; // Firestore document ID or SQLite row ID (converted to String for consistency)

    // Basic details
    private String name;
    private String city;
    private String notes;

    // Photos (as local URIs in SQLite or remote URLs in Firestore)
    private List<String> photoPaths;

    // Extra metadata
    private boolean visited; // Whether the user has marked this place as visited
    private float rating; // User’s personal rating (0–5 stars typically)
    private double latitude; // Stored for map integration
    private double longitude;
    private String distance; // Pre-computed distance string (e.g., "2.5 km away")

    /**
     * Full constructor:
     * - Used when creating new DreamPlace objects programmatically (e.g., after saving/fetching).
     * - Parameters include all core fields.
     */
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

    // ----- Getters -----
    // Provide read access to private fields (required by adapters, UI, Firestore mapping, etc.)
    public String getId() {
        return id;
    }
    public String getName() {
        return name;
    }
    public String getCity() {
        return city;
    }
    public String getNotes() {
        return notes;
    }
    public List<String> getPhotoPaths() {
        return photoPaths;
    }
    public boolean isVisited() {
        return visited;
    }
    public float getRating() {
        return rating;
    }
    public double getLatitude() {
        return latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public String getDistance() {
        return distance;
    }

    // ----- Setters -----
    // Allow updating model properties (important for editing Dream Places).
    public void setId(String id) {
        this.id = id;
    }
    public void setName(String name) {
        this.name = name;
    }
    public void setCity(String city) {
        this.city = city;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public void setPhotoPaths(List<String> photoPaths) {
        this.photoPaths = photoPaths;
    }
    public void setVisited(boolean visited) {
        this.visited = visited;
    }
    public void setRating(float rating) {
        this.rating = rating;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public void setDistance(String distance) {
        this.distance = distance;
    }
}