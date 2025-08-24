package com.s92086882.mydreamplacewishlist;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.List;

/**
 * RecyclerView.Adapter for a horizontal gallery of photos.
 * -
 * Responsibilities:
 * - Binds a List<Uri> to ImageViews using Glide (supports both content:// and file://).
 * - Exposes a click listener callback so the parent screen can open a viewer or handle deletes.
 * -
 * Notes:
 * - For file:// URIs, we pass a java.io.File to Glide for efficient decoding.
 * - For content:// (SAF / MediaStore) or https:// (remote) URIs, we pass the Uri directly.
 * - ViewHolder pattern minimizes findViewById calls for smooth scrolling.
 */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final List<Uri> photoUris; // Backing dataset (URIs to local files or content providers)
    private OnPhotoClickListener listener; // Optional click callback

    public PhotoAdapter(List<Uri> photoUris) {
        this.photoUris = photoUris;
    }

    /** Click contract: parent gets index + Uri of the tapped photo. */
    public interface OnPhotoClickListener {
        void onPhotoClick(int position, Uri photoUri);
    }

    /** Setter to register a click listener from the parent (e.g., detail screen). */
    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.listener = listener;
    }

    /** Inflate the item view for a single photo tile. */
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    /** Bind a photo Uri into the ImageView via Glide. */
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri uri = photoUris.get(position);

        // If the Uri scheme is "file", convert to File so Glide reads from disk directly.
        // Otherwise (content/http/https), pass the Uri as-is.
        Glide.with(holder.imageView.getContext())
                .load("file".equals(uri.getScheme()) ? new File(uri.getPath()) : uri)
                .into(holder.imageView);

        // Forward click events to the registered listener with index + Uri.
        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(position, uri);
            }
        });
    }

    /** Number of photos in the gallery. */
    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    /** Holds the ImageView reference for a single photo tile. */
    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoImageView);
        }
    }
}