package com.example.ttv;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ttv.chatbox.ObjectData;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;


public class ObjectListActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ObjectListAdapter objectListAdapter;
    private List<ObjectData> objectList;
    private int selectedPosition = RecyclerView.NO_POSITION; // Initialize with an invalid position
    private RecyclerView searchResultsRecyclerView;
    private SearchResultsAdapter searchResultsAdapter; // Tạo adapter cho RecyclerView
    private List<String> searchResults;
    ImageButton save, search, send;
    EditText EditName;

    // Define a method to perform image search for a given object name


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.item_object);

        recyclerView = findViewById(R.id.recycler_View);
        save = findViewById(R.id.save_button);
        search = findViewById(R.id.search_button);
        send = findViewById(R.id.send_button);
        EditName = findViewById(R.id.editTextText);


        // Retrieve the ParcelableArrayListExtra after you've received the intent
        Intent intent = getIntent();
        objectList = intent.getParcelableArrayListExtra("objectList");


        // Use ArrayList when setting up the adapter
        objectListAdapter = new ObjectListAdapter(new ArrayList<>(objectList));

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(objectListAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
        // Khởi tạo RecyclerView và adapter
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchResults = new ArrayList<>();
        searchResultsAdapter = new SearchResultsAdapter(searchResults);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(searchResultsAdapter);
        objectListAdapter.setNameClickListener(objectData -> {
            selectedPosition = objectList.indexOf(objectData); // Save the selected position
            EditName.setText(objectData.getObjName());
        });

        objectListAdapter.setOnImageClickListener(objImage -> {
            showZoomedImageDialog(objImage);
        });


        save.setOnClickListener(view -> {
            if (selectedPosition != RecyclerView.NO_POSITION) {
                // Ensure the selected position is valid
                String newName = EditName.getText().toString().trim();
                if (!newName.isEmpty()) {
                    // Update the object at the selected position
                    ObjectData selectedObject = objectList.get(selectedPosition);
                    selectedObject.setObjName(newName);
                    // Notify the adapter that data has changed
                    objectListAdapter.notifyItemChanged(selectedPosition);
                    // Reset selected position
                    selectedPosition = RecyclerView.NO_POSITION;
                    // Clear the EditName text
                    EditName.setText("");
                }
            }
        });
        search.setOnClickListener(v -> {
            // Get the text from the EditText
            String searchText = EditName.getText().toString().trim();

            // Check if the search text is not empty
            if (!searchText.isEmpty()) {
                // Perform image search using the search text
                performImageSearch(searchText);
            } else {
                // If search text is empty, show a message to the user or handle it appropriately
                Toast.makeText(ObjectListActivity.this, "Please enter a search term", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void performImageSearch(String query) {
        // Replace "YOUR_API_KEY" and "YOUR_CX" with your actual API key and Custom Search Engine ID
        String apiKey = "AIzaSyAuvp2QpjJ1Tg_vWeBGvW86_1Y-KtvCWvI";
        String cx = "16cb5a7ea28cc45e3";
        int numResults = 3;

        String apiUrl = "https://www.googleapis.com/customsearch/v1?q="
                + query + "&key=" + apiKey + "&cx=" + cx + "&searchType=image&num=" + numResults;

        // Tạo một đối tượng ImageSearchTask và thực hiện tìm kiếm ảnh
        ImageSearchTask imageSearchTask = new ImageSearchTask(this, apiUrl);
        imageSearchTask.execute();
    }
    private static List<String> parseImageUrlsFromInputStream(InputStream in) {
        List<String> imageUrls = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder responseStringBuilder = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                responseStringBuilder.append(line);
            }

            JSONObject jsonResponse = new JSONObject(responseStringBuilder.toString());
            JSONArray itemsArray = jsonResponse.optJSONArray("items");

            if (itemsArray != null) {
                for (int i = 0; i < itemsArray.length(); i++) {
                    JSONObject item = itemsArray.optJSONObject(i);
                    if (item != null) {
                        JSONObject imageObject = item.optJSONObject("image");
                        if (imageObject != null) {
                            String imageUrl = imageObject.optString("thumbnailLink");
                            if (!imageUrl.isEmpty()) {
                                imageUrls.add(imageUrl);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return imageUrls;
    }


    private static class ImageSearchTask extends AsyncTask<Void, Void, List<String>> {
        private WeakReference<ObjectListActivity> activityReference;
        private String apiUrl;

        ImageSearchTask(ObjectListActivity activity, String apiUrl) {
            activityReference = new WeakReference<>(activity);
            this.apiUrl = apiUrl;
        }

        @Override
        protected List<String> doInBackground(Void... voids) {
            try {
                ObjectListActivity activity = activityReference.get();
                if (activity == null || activity.isFinishing()) {
                    // Activity is no longer available, return null or an empty list
                    return null;
                }

                URL url = new URL(apiUrl);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");

                // Read the response
                InputStream in = urlConnection.getInputStream();

                // Parse the response and get image URLs
                List<String> imageUrls = parseImageUrlsFromInputStream(in);

                // Print the API response (for debugging)
                Log.d("ImageSearchTask", "API Response: " + convertInputStreamToString(in));

                // Return the list of image URLs
                return imageUrls;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
        private String convertInputStreamToString(InputStream inputStream) {
            Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }

        @Override
        protected void onPostExecute(List<String> imageUrls) {
            ObjectListActivity activity = activityReference.get();
            if (activity != null && !activity.isFinishing()) {
                // Display images using the imageUrls
                // You might want to use an image loading library like Picasso or Glide
                // to load and display images efficiently
                activity.displayImages(imageUrls);
            }
        }

    }
    private void displayImages(List<String> imageUrls) {
        if (imageUrls != null) {
            // Update the searchResults list and notify the adapter
            searchResults.clear();
            searchResults.addAll(imageUrls);
            searchResultsAdapter.notifyDataSetChanged();

            // Show the RecyclerView
            searchResultsRecyclerView.setVisibility(View.VISIBLE);
        } else {
            // Handle the case where imageUrls is null
            Toast.makeText(this, "Error loading images. Please check your network connection.", Toast.LENGTH_SHORT).show();
            Log.e("ImageSearchTask", "Error loading images. Image URLs are null.");
        }
    }











    // phóng to ảnh
    private void showZoomedImageDialog(Bitmap objImage) {
        // Create a custom layout for the dialog
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_zoom_image, null);

        // Find the ImageView in the custom layout
        ImageView imageView = dialogView.findViewById(R.id.imageViewZoom);

        // Set the image to the ImageView
        imageView.setImageBitmap(objImage);

        // Create an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());

        // Show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }


}
