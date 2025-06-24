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

    private final List<Uri> photoUris;
    private OnPhotoClickListener listener;

    public PhotoAdapter(List<Uri> photoUris) {
        this.photoUris = photoUris;
    }

    public interface OnPhotoClickListener {
        void onPhotoClick(int position, Uri photoUri);
    }

    public void setOnPhotoClickListener(OnPhotoClickListener listener) {
        this.listener = listener;
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

        Glide.with(holder.imageView.getContext())
                .load("file".equals(uri.getScheme()) ? new File(uri.getPath()) : uri)
                .into(holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPhotoClick(position, uri);
            }
        });
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