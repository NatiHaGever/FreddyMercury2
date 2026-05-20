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

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<ForumComment> commentList;

    public CommentAdapter(List<ForumComment> commentList) {
        this.commentList = commentList;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.comment_item, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        ForumComment comment = commentList.get(position);
        holder.authorText.setText(comment.authorName);
        holder.commentText.setText(comment.text);

        if (comment.timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault());
            // Fixed: comment.timestamp is now a Date object, so no need for .toDate()
            holder.timeText.setText(sdf.format(comment.timestamp));
        }
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView authorText, commentText, timeText;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            authorText = itemView.findViewById(R.id.commentAuthorText);
            commentText = itemView.findViewById(R.id.commentText);
            timeText = itemView.findViewById(R.id.commentTimeText);
        }
    }
}
