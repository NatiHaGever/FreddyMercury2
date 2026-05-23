package com.example.freddymercury;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddTask extends AppCompatActivity {

    private static final String IMGBB_API_KEY = "16784a4f98a01b6293ec3e2e11f642c4";

    EditText taskTitleInput;
    TextView dueDateText;
    Button saveTaskBtn;
    EditText taskDescription;

    Button btnSelectImage;
    ImageView imgPreview;
    Uri selectedImageUri = null;

    String selectedDate = "";
    String groupId = null;

    FirebaseFirestore db;
    FirebaseAuth auth;
    OkHttpClient httpClient;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    imgPreview.setImageURI(selectedImageUri);
                    imgPreview.setVisibility(View.VISIBLE);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        groupId = getIntent().getStringExtra("groupId");

        taskDescription = findViewById(R.id.editDescription);
        taskTitleInput = findViewById(R.id.taskTitleInput);
        dueDateText = findViewById(R.id.dueDateText);
        saveTaskBtn = findViewById(R.id.saveTaskBtn);
        btnSelectImage = findViewById(R.id.btnSelectImage);
        imgPreview = findViewById(R.id.imgPreview);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        httpClient = new OkHttpClient();

        dueDateText.setOnClickListener(v -> showDatePicker());
        saveTaskBtn.setOnClickListener(v -> handleSaveSequence());

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this,
                (view, year, month, day) -> {
                    // Output format "d/M/yyyy" matches NotificationScheduler expectation
                    selectedDate = day + "/" + (month + 1) + "/" + year;
                    dueDateText.setText(selectedDate);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void handleSaveSequence() {
        String title = taskTitleInput.getText().toString().trim();
        if (title.isEmpty() || selectedDate.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedImageUri == null) {
            saveTaskToFirestore("");
            return;
        }

        uploadImageToImgBB();
    }

    private void uploadImageToImgBB() {
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        String base64Image = convertUriToBase64(selectedImageUri);
        if (base64Image == null) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody formBody = new FormBody.Builder()
                .add("image", base64Image)
                .build();

        Request request = new Request.Builder()
                .url("https://api.imgbb.com/1/upload?key=" + IMGBB_API_KEY)
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, java.io.IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(AddTask.this, "Upload failed, saving text only", Toast.LENGTH_SHORT).show();
                    saveTaskToFirestore("");
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws java.io.IOException {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String rawJsonResponse = response.body().string();
                        JSONObject jsonObject = new JSONObject(rawJsonResponse);
                        String directImageUrl = jsonObject.getJSONObject("data").getString("url");
                        runOnUiThread(() -> saveTaskToFirestore(directImageUrl));
                    } else {
                        runOnUiThread(() -> saveTaskToFirestore(""));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> saveTaskToFirestore(""));
                }
            }
        });
    }

    private String convertUriToBase64(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while (inputStream != null && (len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();
            return Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (Exception e) {
            return null;
        }
    }

    private void saveTaskToFirestore(String imageUrl) {
        String title = taskTitleInput.getText().toString().trim();
        String desc = taskDescription.getText().toString().trim();
        String userId = auth.getCurrentUser().getUid();

        Task task = new Task(title, selectedDate, userId, desc);
        task.imageUrl = imageUrl;

        if (groupId != null && !groupId.isEmpty()) {
            task.groupId = groupId;
        } else {
            task.groupId = "personal";
        }

        db.collection("tasks")
                .add(task)
                .addOnSuccessListener(doc -> {
                    task.docId = doc.getId();
                    // FIXED: Call the updated scheduleTaskAlert method
                    NotificationScheduler.scheduleTaskAlert(AddTask.this, task);
                    Toast.makeText(this, "Task saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error saving task", Toast.LENGTH_SHORT).show();
                });
    }
}