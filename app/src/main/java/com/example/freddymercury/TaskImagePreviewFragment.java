package com.example.freddymercury;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide; // Or your preferred image handling package
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class TaskImagePreviewFragment extends BottomSheetDialogFragment {

    private static final String ARG_TITLE = "task_title";
    private static final String ARG_URL = "image_url";

    public static TaskImagePreviewFragment newInstance(String title, String imageUrl) {
        TaskImagePreviewFragment fragment = new TaskImagePreviewFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task_image_preview, container, false);

        TextView titleText = view.findViewById(R.id.previewTaskTitle);
        ImageView imageView = view.findViewById(R.id.previewImageView);

        if (getArguments() != null) {
            String title = getArguments().getString(ARG_TITLE);
            String url = getArguments().getString(ARG_URL);

            titleText.setText(title);

            // Pop the cloud image smoothly via storage URL
            if (url != null && !url.isEmpty()) {
                Glide.with(this).load(url).into(imageView);
            }
        }
        return view;
    }
}