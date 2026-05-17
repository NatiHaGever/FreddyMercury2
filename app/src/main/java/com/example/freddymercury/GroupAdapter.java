package com.example.freddymercury;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private final List<Group> groupList;
    private final OnGroupClickListener listener;

    public interface OnGroupClickListener {
        void onGroupClick(Group group);
    }

    public GroupAdapter(List<Group> groupList, OnGroupClickListener listener) {
        this.groupList = groupList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_item, parent, false);
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        Group group = groupList.get(position);
        holder.groupName.setText(group.groupName);
        holder.groupCode.setText("Code: " + group.groupCode);
        holder.memberCount.setText(group.members.size() + " Members");

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onGroupClick(group);
            }
        });
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, groupCode, memberCount;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.groupNameText);
            groupCode = itemView.findViewById(R.id.groupCodeText);
            memberCount = itemView.findViewById(R.id.memberCountText);
        }
    }
}
