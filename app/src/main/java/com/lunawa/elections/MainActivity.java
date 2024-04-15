package com.lunawa.elections;

import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import retrofit2.Retrofit;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.http.POST;
import retrofit2.http.Headers;
import retrofit2.http.Body;

import com.google.gson.annotations.SerializedName;
import android.view.LayoutInflater;


public class MainActivity extends AppCompatActivity {
    private SharedPreferences sharedPreferences;
    private TextView statusTextView;
    private Button loginButton;
    private Retrofit retrofit;

    public class PasswordRequest {
        @SerializedName("password")
        private String password;

        public PasswordRequest(String password) {
            this.password = password;
        }
    }
    public interface UserApi {
        @Headers("Content-Type: application/json")
        @POST("auth/")
        Call<Void> authenticateWithPassword(@Body PasswordRequest passwordRequest);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginButton = findViewById(R.id.submit);
        Button serverButton = findViewById(R.id.server_button);
        EditText passwordEditText = findViewById(R.id.password);
        statusTextView = findViewById(R.id.status);
        statusTextView.setVisibility(View.GONE);
        sharedPreferences = getSharedPreferences(getString(R.string.shared_name), MODE_PRIVATE);
        saveSharedPref("server_url", sharedPreferences.getString("server_url", getString(R.string.server_url)));

        retrofit = RetrofitClient.getRetrofitInstance(sharedPreferences.getString("server_url", ""));

        loginButton.setOnClickListener(v -> {
            String enteredPassword = passwordEditText.getText().toString();
            authenticatePassword(enteredPassword);
        });

        serverButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d("serverButton", "Clicked Me!");
                openUrlFragement();
                return true;
            }
        });

        if (sharedPreferences.getBoolean("LoginSuccess", false)) {
            updateStatus("Success", Color.GREEN);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sharedPreferences.getBoolean("LoginSuccess", false)) {
            updateStatus("Success", Color.GREEN);
        }
    }

    public void saveSharedPref(String key, boolean value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public void saveSharedPref(String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void updateStatus(String status, int color) {
        statusTextView.setVisibility(View.VISIBLE);
        statusTextView.setText(status);
        statusTextView.setTextColor(color);

        if (status.equalsIgnoreCase("Success")) {
            startActivity(new Intent(this, CameraActivity.class));
            finish();
        }
    }

    private void authenticatePassword(String password) {
        loginButton.setVisibility(View.GONE);
        UserApi userApi = retrofit.create(UserApi.class);
        PasswordRequest passwordRequest = new PasswordRequest(password);
        Call<Void> call = userApi.authenticateWithPassword(passwordRequest);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    saveSharedPref("LoginSuccess", true);
                    updateStatus("Success", Color.GREEN);
                } else {
                    saveSharedPref("LoginSuccess", false);
                    updateStatus("Wrong Password", Color.RED);
                    Toast.makeText(MainActivity.this, "Wrong Password", Toast.LENGTH_SHORT).show();
                }
                loginButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                saveSharedPref("LoginSuccess", false);
                updateStatus("Server Error", Color.RED);
                Toast.makeText(MainActivity.this, "Server Error", Toast.LENGTH_SHORT).show();
                loginButton.setVisibility(View.VISIBLE);
            }
        });
//        updateStatus("Success", Color.GREEN);
    }

    private void openUrlFragement(){
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.fragment_url, null);
        final EditText yesEt = (EditText) dialogView.findViewById(R.id.urlEditText);
        yesEt.setText(sharedPreferences.getString("server_url", ""));
        String message = "Enter URL - http://10.0.2.2:8000/";

        final AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setTitle("Server")
                .setView(dialogView)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton("Set New", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String newUrl = yesEt.getText().toString();
                        saveSharedPref("server_url", newUrl);
                        retrofit = RetrofitClient.getRetrofitInstance(sharedPreferences.getString("server_url", ""));
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveSharedPref("server_url", getString(R.string.server_url));
                        dialog.dismiss();
                    }
                })
                .create();
        alertDialog.show();
    }
}