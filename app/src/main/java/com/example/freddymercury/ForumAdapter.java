package com.example.freddymercury;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ForumAdapter extends RecyclerView.Adapter<ForumAdapter.ForumViewHolder> {

    private final List<ForumPost> postList;
    private final OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(ForumPost post);
    }

    public ForumAdapter(List<ForumPost> postList, OnPostClickListener listener) {
        this.postList = postList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ForumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.forum_post_item, parent, false);
        return new ForumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ForumViewHolder holder, int position) {
        ForumPost post = postList.get(position);
        holder.titleText.setText(post.title);
        holder.authorText.setText("By: " + post.authorName);
        holder.descriptionText.setText(post.description);

        if (post.timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            // Fixed: post.timestamp is now a Date object
            holder.timeText.setText(sdf.format(post.timestamp));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class ForumViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, authorText, descriptionText, timeText;

        public ForumViewHolder(@NonNull View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.postTitleText);
            authorText = itemView.findViewById(R.id.postAuthorText);
            descriptionText = itemView.findViewById(R.id.postDescriptionText);
            timeText = itemView.findViewById(R.id.postTimeText);
        }
    }
}
