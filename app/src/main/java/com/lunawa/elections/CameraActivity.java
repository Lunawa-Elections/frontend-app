package com.lunawa.elections;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;

import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.Callback;
import retrofit2.Response;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import retrofit2.http.Path;

import android.util.Log;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class CameraActivity extends AppCompatActivity {

    private Uri photoURI;
    private ImageView imageView;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int PERMISSION_REQUEST_CAMERA = 101;
    private TextView statusTextView;
    private TextView successCounter;
    private Button captureButton;
    private Retrofit retrofit;
    private String androidId;

    public interface UploadService {
        @Multipart
        @POST("upload/") // Update with your upload endpoint
        Call<ResponseBody> uploadImage(@Part MultipartBody.Part image);
    }

    public interface CounterService {
        @GET("counter/{android_id}")
        Call<ResponseBody> getCounter(@Path("android_id") String androidId);
    }

    public interface ResetService {
        @GET("delete/{android_id}")
        Call<ResponseBody> getCounter(@Path("android_id") String androidId);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        imageView = findViewById(R.id.imageBallotView);
        captureButton = findViewById(R.id.click_image);
        Button resetButton = findViewById(R.id.reset_button);
        Button logoutButton = findViewById(R.id.logout);
        statusTextView = findViewById(R.id.status_counter);
        statusTextView.setVisibility(View.GONE);

        retrofit = RetrofitClient.getRetrofitInstance();
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        successCounter = findViewById(R.id.success_counter);
        getCounter();

        captureButton.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(CameraActivity.this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(CameraActivity.this, new String[]{android.Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
            }
        });

        resetButton.setOnClickListener(this::reset);
        successCounter.setOnClickListener(this::getCounter);
        logoutButton.setOnClickListener(this::logout);
    }

    private void getCounter(View view) {
        statusTextView.setVisibility(View.GONE);
        getCounter();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    @Override
    public void onResume() {
        super.onResume();
        getCounter();
    }

    private void logout(View view){
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.shared_name), MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("LoginSuccess", false);
        editor.apply();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class); // Use your main activity class
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void reset(View view) {
        statusTextView.setVisibility(View.GONE);
        imageView.setImageBitmap(null);
        photoURI = null;

        ResetService service = retrofit.create(ResetService.class);
        Call<ResponseBody> call = service.getCounter(androidId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    updateStatus("Reset Successful", Color.GREEN);
                } else {
                    updateStatus("Reset Failed", Color.RED);
                    Toast.makeText(CameraActivity.this, "Reset Failed", Toast.LENGTH_SHORT).show();
                }
                getCounter();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateStatus("Server Error ", Color.RED);
                getCounter();
            }
        });
    }

    private void getCounter() {
        CounterService service = retrofit.create(CounterService.class);
        Call<ResponseBody> call = service.getCounter(androidId);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    try {
                        String result = response.body().string();
                        successCounter.setText("Counter: " + result);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    updateStatus("Counter not Fetched", Color.RED);
                    Toast.makeText(CameraActivity.this, "Counter not Fetched", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateStatus("Server Error", Color.RED);
            }
        });
    }

    private void updateStatus(String status, int color) {
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(status);
        statusTextView.setTextColor(color);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            }
            else {
                Toast.makeText(this, "Grant Camera Permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Uri createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "ballot_" + androidId + "_" + timeStamp + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ElectionImages");
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(MediaStore.Images.Media.IS_PENDING, 0);
                    getContentResolver().update(uri, values, null, null);
                }
            } catch (IOException e) {
                e.printStackTrace();
                getContentResolver().delete(uri, null, null);
                Toast.makeText(this, "Internal Error", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        return uri;
    }

    private void openCamera() {
        captureButton.setVisibility(View.GONE);
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            photoURI = createImageFile();
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void uploadImage(Uri imageUri) {
        if (imageUri == null) {
            Log.d("Upload", "photoURI is null or empty");
            Toast.makeText(this, "No photo to upload", Toast.LENGTH_SHORT).show();
            return;
        }

        String filePath = null;
        try (Cursor cursor = getContentResolver().query(imageUri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                filePath = cursor.getString(columnIndex);
                if (filePath == null){
                    throw new IllegalStateException("File path is null");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error accessing image file", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            Log.e("Upload", "File does not exist: " + file.getAbsolutePath());
            Toast.makeText(this, "Selected image does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", file.getName(), requestFile);
        UploadService uploadService = retrofit.create(UploadService.class);

        Call<ResponseBody> call = uploadService.uploadImage(imagePart);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(response.body().byteStream());
                    runOnUiThread(() -> imageView.setImageBitmap(bitmap));
                    updateStatus("Success", Color.GREEN);
                    Toast.makeText(CameraActivity.this, "Image Uploaded successfully", Toast.LENGTH_SHORT).show();
                } else {
                    updateStatus("Invalid Image", Color.RED);
                    Toast.makeText(CameraActivity.this, "Need image of ballot", Toast.LENGTH_SHORT).show();
                }
                photoURI = null;
                getCounter();
                captureButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                updateStatus("Server Error", Color.RED);
                captureButton.setVisibility(View.VISIBLE);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(photoURI);
            statusTextView.setVisibility(View.GONE);
            uploadImage(photoURI);
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE){
            captureButton.setVisibility(View.VISIBLE);
        }
    }
}