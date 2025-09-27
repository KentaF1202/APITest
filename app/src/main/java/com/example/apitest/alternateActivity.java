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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class alternateActivity extends AppCompatActivity {

    private static final String TAG = "FastAPIClient";
    private TextView textView; // Member variable to update status
    private ImageView imageView; // Member variable for the image

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

        Button ultimateButton = findViewById(R.id.ultimateButton);
        this.textView = findViewById(R.id.textView);
        this.imageView = findViewById(R.id.imageView);

        ultimateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the old status and start fetching
                textView.setText("Status: Fetching image...");
                new Thread(this::fetchImage).start();
            }

            // Renamed method for clarity:
            private void fetchImage() {
                // Ensure this URL is correct for your local server
                String url = "http://10.108.254.228:8000/image";

                OkHttpClient client = new OkHttpClient();

                Request request = new Request.Builder()
                        .url(url)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {

                        // 1. Get the raw image data as a byte array
                        byte[] imageBytes = response.body().bytes();

                        // 2. Decode the byte array into an Android Bitmap
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

                        if (bitmap != null) {
                            Log.d(TAG, "Image successfully decoded. Width: " + bitmap.getWidth());

                            // 3. Update the UI on the main thread
                            runOnUiThread(() -> {
                                imageView.setImageBitmap(bitmap);
                                textView.setText("Status: Image Loaded Successfully!");
                            });
                        } else {
                            // If decoding fails (e.g., corrupted data, not an image)
                            Log.e(TAG, "Failed to decode image bytes into Bitmap.");
                            runOnUiThread(() -> textView.setText("Status: Failed to decode image."));
                        }

                    } else {
                        final int code = response.code();
                        Log.e(TAG, "Request failed with code: " + code);
                        final String errorBody = response.body() != null ? response.body().string() : "No body";
                        Log.e(TAG, "Error Body: " + errorBody);

                        runOnUiThread(() -> textView.setText("Status: Request failed (Code: " + code + ")"));
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Network error: " + e.getMessage());
                    runOnUiThread(() -> textView.setText("Status: Network error: " + e.getMessage()));
                }
                // Removed JSONException catch as we are no longer expecting JSON by default
            }
        });
    }
}