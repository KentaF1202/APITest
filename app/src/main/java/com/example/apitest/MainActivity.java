package com.example.apitest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FastAPIClient";
    private TextView textView;
    private ZoomableImageView imageView;
    //private ImageView imageView; // Only need one ImageView now
    private Button ultimateButton;

    // 1. Store the URLs and the current index
    private List<String> imageUrls = new ArrayList<>();
    private int currentImageIndex = 0;

    // Base URL needed to form the complete image path
    private final String BASE_URL = "http://10.108.254.228:8000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ultimateButton = findViewById(R.id.ultimateButton);
        textView = findViewById(R.id.textView);
        imageView = findViewById(R.id.imageView); // Assuming you keep the single ImageView from before

        // Disable the button initially until the image list is loaded
        ultimateButton.setEnabled(false);
        ultimateButton.setText("Loading Images...");

        // Start the process by fetching the list of image paths
        new Thread(this::fetchImageList).start();

        // 2. Set up the click listener for cycling
        ultimateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if the list is empty
                if (imageUrls.isEmpty()) {
                    textView.setText("Status: Image list is empty.");
                    return;
                }

                // CRUCIAL: Load the next image
                loadNextImage();
            }
        });
    }

    // New method to load and display the image at the currentImageIndex
    private void loadNextImage() {
        // Calculate the next index, cycling back to 0 if we reach the end
        currentImageIndex = (currentImageIndex + 1) % imageUrls.size();

        // Get the full URL
        String relativePath = imageUrls.get(currentImageIndex);
        String fullUrl = BASE_URL + relativePath;

        textView.setText("Status: Displaying image " + (currentImageIndex + 1) + " of " + imageUrls.size());

        // Use Picasso to load the image into the single ImageView
        Picasso.get()
                .load(fullUrl)
                .into(imageView);
    }

    // Updated method to fetch the list of image paths
    private void fetchImageList() {
        String url = BASE_URL + "/image-list";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String jsonString = response.body().string();
                JSONArray jsonArray = new JSONArray(jsonString);

                // Clear and populate the class-level list (imageUrls)
                imageUrls.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    // Store the relative path (e.g., "/images/img1.png")
                    imageUrls.add(jsonArray.getString(i));
                }

                runOnUiThread(() -> {
                    if (imageUrls.isEmpty()) {
                        textView.setText("Status: Server returned an empty list.");
                        ultimateButton.setEnabled(false);
                        ultimateButton.setText("No Images Found");
                    } else {
                        // Success: Enable the button and load the first image (index -1 + 1 = 0)
                        ultimateButton.setEnabled(true);
                        ultimateButton.setText("Next Image");
                        currentImageIndex = -1; // Set to -1 so the first loadNextImage() call loads index 0
                        loadNextImage();
                    }
                });

            } else {
                final int code = response.code();
                // ... (error logging)
                runOnUiThread(() -> textView.setText("Status: List request failed (Code: " + code + ")"));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error fetching/parsing list: " + e.getMessage());
            runOnUiThread(() -> textView.setText("Status: Error fetching/parsing list."));
        }
    }
}