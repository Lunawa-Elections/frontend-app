package com.lunawa.elections;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public class RetrofitClient {

    private static Retrofit retrofitInstance;
    private static String BASE_URL;

    public static Retrofit getRetrofitInstance() {
        return getRetrofitInstance(""); // Pass the default value
    }

    public static Retrofit getRetrofitInstance(String base_url) {
        if (!(retrofitInstance != null && (BASE_URL.equals(base_url) || base_url.isEmpty()))) {

            RetrofitClient.BASE_URL = base_url;
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            httpClient.addInterceptor(logging);

            retrofitInstance = new Retrofit.Builder()
                    .baseUrl(base_url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }
        return retrofitInstance;
    }
}