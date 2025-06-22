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
 * Adapter class for displaying a list of selected photo URIs or file paths in a RecyclerView.
 */
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private final List<Uri> photoUris; // For guest users: internal file URIs or original content URIs

    public PhotoAdapter(List<Uri> photoUris) {
        this.photoUris = photoUris;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri uri = photoUris.get(position);

        if (uri.getScheme() != null && uri.getScheme().equals("file")) {
            // File-based URI from internal storage
            Glide.with(holder.imageView.getContext())
                    .load(new File(uri.getPath()))
                    .into(holder.imageView);
        } else {
            // Content URI or remote URI
            Glide.with(holder.imageView.getContext())
                    .load(uri)
                    .into(holder.imageView);
        }
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.photoImageView);
        }
    }
}
