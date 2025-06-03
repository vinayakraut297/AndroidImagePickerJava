package com.example.imagepicker;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ImageView imageView;
    private TextView textResults;
    private CircularProgressIndicator progressIndicator;
    private View resultsCard;
    private Uri image_uri;
    private TextRecognizer textRecognizer;

    // Gallery Launcher
    ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri img_url = result.getData().getData();
                        loadAndScanImage(img_url);
                    }
                }
            });

    // Camera Launcher
    ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && image_uri != null) {
                        loadAndScanImage(image_uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize ML Kit
        textRecognizer = TextRecognition.getClient(new TextRecognizerOptions.Builder().build());

        // Initialize views
        imageView = findViewById(R.id.imageView);
        textResults = findViewById(R.id.textResults);
        progressIndicator = findViewById(R.id.progressIndicator);
        resultsCard = findViewById(R.id.resultsCard);

        // Setup click listeners
        findViewById(R.id.button).setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryActivityResultLauncher.launch(galleryIntent);
        });

        findViewById(R.id.button2).setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            }
        });
    }

    private void loadAndScanImage(Uri imageUri) {
        // Show loading indicator
        progressIndicator.setVisibility(View.VISIBLE);
        resultsCard.setVisibility(View.GONE);

        // Load image
        Glide.with(this)
                .load(imageUri)
                .into(imageView);

        try {
            // Create InputImage
            InputImage image = InputImage.fromFilePath(this, imageUri);

            // Perform text recognition
            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        StringBuilder result = new StringBuilder();
                        for (Text.TextBlock block : text.getTextBlocks()) {
                            result.append(block.getText()).append("\n\n");
                        }
                        textResults.setText(result.toString().trim());
                        progressIndicator.setVisibility(View.GONE);
                        resultsCard.setVisibility(View.VISIBLE);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Text recognition failed", e);
                        Toast.makeText(this, "Text recognition failed: " + e.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                        progressIndicator.setVisibility(View.GONE);
                    });

        } catch (IOException e) {
            Log.e(TAG, "Error loading image", e);
            Toast.makeText(this, "Error loading image: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
            progressIndicator.setVisibility(View.GONE);
        }
    }

    private boolean checkCameraPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, 112);
                return false;
            }
        }
        return true;
    }

    private void openCamera() {
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From the Camera");
            image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            
            if (image_uri == null) {
                Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
            cameraActivityResultLauncher.launch(cameraIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            Toast.makeText(this, "Error opening camera: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 112) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

