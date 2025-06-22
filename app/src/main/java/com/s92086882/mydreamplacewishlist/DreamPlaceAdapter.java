package com.s92086882.mydreamplacewishlist;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class DreamPlaceAdapter extends RecyclerView.Adapter<DreamPlaceAdapter.ViewHolder> {

    private final Context context;
    private List<DreamPlace> dreamPlaceList;
    private final OnItemClickListener listener;

    // Interface for item clicks
    public interface OnItemClickListener {
        void onItemClick(DreamPlace place);
    }

    public DreamPlaceAdapter(Context context, List<DreamPlace> dreamPlaceList, OnItemClickListener listener) {
        this.context = context;
        this.dreamPlaceList = dreamPlaceList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_dream_place, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DreamPlace place = dreamPlaceList.get(position);

        // Load first photo
        if (place.getPhotoPaths() != null && !place.getPhotoPaths().isEmpty()) {
            String photo = place.getPhotoPaths().get(0);
            try {
                Glide.with(context)
                        .load(Uri.parse(photo))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_launcher_background)
                        .into(holder.imageViewPlace);
            } catch (Exception e) {
                holder.imageViewPlace.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.imageViewPlace.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.textViewPlaceName.setText(place.getName());
        holder.textViewCity.setText(place.getCity());

        // Show distance
        if (place.getDistance() != null && !place.getDistance().isEmpty()) {
            holder.textViewDistance.setText(place.getDistance());
            holder.textViewDistance.setVisibility(View.VISIBLE);
        } else {
            holder.textViewDistance.setVisibility(View.GONE);
        }

        // Visited + Rating
        if (place.isVisited()) {
            holder.visitedContainer.setVisibility(View.VISIBLE);
            holder.textViewVisited.setText("Visited");

            holder.ratingContainer.setVisibility(View.VISIBLE);
            holder.textViewRating.setText(String.valueOf(place.getRating()));
        } else {
            holder.visitedContainer.setVisibility(View.GONE);
            holder.ratingContainer.setVisibility(View.GONE);
        }

        // Click handler
        holder.itemView.setOnClickListener(v -> listener.onItemClick(place));
    }

    @Override
    public int getItemCount() {
        return dreamPlaceList.size();
    }

    public void updateList(List<DreamPlace> updatedList) {
        this.dreamPlaceList = updatedList;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPlace, iconVisited, iconStar;
        TextView textViewPlaceName, textViewCity, textViewDistance, textViewVisited, textViewRating;
        View visitedContainer, ratingContainer;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPlace = itemView.findViewById(R.id.imageViewPlacePhoto);
            textViewPlaceName = itemView.findViewById(R.id.textViewPlaceName);
            textViewCity = itemView.findViewById(R.id.textViewCity);
            textViewDistance = itemView.findViewById(R.id.textViewDistance);

            iconVisited = itemView.findViewById(R.id.imageViewVisitedIcon);
            textViewVisited = itemView.findViewById(R.id.textViewVisited);
            visitedContainer = (View) textViewVisited.getParent();

            iconStar = itemView.findViewById(R.id.imageViewRatingIcon);
            textViewRating = itemView.findViewById(R.id.textViewRating);
            ratingContainer = (View) textViewRating.getParent();
        }
    }
}