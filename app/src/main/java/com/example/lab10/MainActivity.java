package com.example.lab10;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.room.Room;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private AppDatabase db;
    private GoogleMap mMap;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spot-db").build();

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        else {
            initMap();
            findViewById(R.id.btnToFavorite).setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, FavoriteActivity.class);
                startActivityForResult(intent, 100);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("lat", 0);
            double lng = data.getDoubleExtra("lng", 0);
            String name = data.getStringExtra("name");

            if (mMap != null) {
                LatLng location = new LatLng(lat, lng);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));
                mMap.addMarker(new MarkerOptions()
                        .position(location)
                        .title(name));
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED)
                    finish();
                else {
                    initMap();
                }
            }
        }
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        // 檢查是否授權定位權限
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // 設定中心與縮放
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(25.034, 121.545), 13));

        mMap.setOnMarkerClickListener(marker -> {
            showCustomDialog(marker);
            return true;
        });


        // 抓 API 並加上標記
        fetchAttractionsAndAddMarkers();
    }

    private void showCustomDialog(Marker marker) {
        View view = getLayoutInflater().inflate(R.layout.custom_dialog, null);
        TextView tvPlaceName = view.findViewById(R.id.tvPlaceName);
        Button btnFavorite = view.findViewById(R.id.btnFavorite);

        String name = marker.getTitle();
        LatLng position = marker.getPosition();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        Button btnDetail = view.findViewById(R.id.btnDetail);

        // marker.setTag(spotDetail); // 請確認你先前有這樣設資料
        SpotDetail spotDetail = (SpotDetail) marker.getTag();
        if (spotDetail != null) {
            tvPlaceName.setText(spotDetail.name + "\n" + spotDetail.address);
        } else {
            // fallback，如果沒資料就用 marker.title
            tvPlaceName.setText(marker.getTitle());
        }

        btnDetail.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("name", spotDetail.name);
            intent.putExtra("address", spotDetail.address);
            intent.putExtra("intro", spotDetail.introduction);
            intent.putStringArrayListExtra("images", new ArrayList<>(spotDetail.imageUrls));
            startActivity(intent);
        });

        // 在 Dialog 開啟時先檢查是否已收藏
        new Thread(() -> {
            FavoriteSpot existing = db.favoriteDao().findByNameAndLatLng(name, position.latitude, position.longitude);

            runOnUiThread(() -> {
                if (existing != null) {
                    btnFavorite.setText("已收藏");
                    btnFavorite.setEnabled(false);
                }

                btnFavorite.setOnClickListener(v -> {
                    // 再次防呆確認（避免 race condition）
                    new Thread(() -> {
                        FavoriteSpot checkAgain = db.favoriteDao().findByNameAndLatLng(name, position.latitude, position.longitude);
                        if (checkAgain == null) {
                            db.favoriteDao().insert(new FavoriteSpot(name, spotDetail.address, position.latitude, position.longitude));

                            runOnUiThread(() -> {
                                Toast.makeText(this, "已加入收藏", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        } else {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "已在收藏中", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            });
                        }
                    }).start();
                });

                dialog.show();
            });
        }).start();
    }

    private void fetchAttractionsAndAddMarkers() {
        Request request = new Request.Builder()
                .url("https://www.travel.taipei/open-api/zh-tw/Attractions/All")
                .addHeader("accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            final Handler mainHandler = new Handler(Looper.getMainLooper());

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("API_ERROR", "API failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (!response.isSuccessful()) {
                    Log.e("API_ERROR", "Unexpected code: " + response);
                    return;
                }

                String responseBody = response.body().string();
                try {
                    JSONObject json = new JSONObject(responseBody);
                    JSONArray dataArray = json.getJSONArray("data");

                    for (int i = 0; i < Math.min(30, dataArray.length()); i++) {
                        JSONObject item = dataArray.getJSONObject(i);

                        JSONArray fileArray = item.optJSONArray("file");
                        List<String> imageUrls = new ArrayList<>();
                        if (fileArray != null) {
                            for (int j = 0; j < fileArray.length(); j++) {
                                JSONObject file = fileArray.getJSONObject(j);
                                imageUrls.add(file.optString("src"));
                            }
                        }

                        String name = item.getString("name");
                        String address = item.getString("address");
                        String intro = item.optString("introduction");

                        double lat = item.optDouble("nlat", 0);
                        double lng = item.optDouble("elong", 0);
                        SpotDetail detail = new SpotDetail(name, address, intro, imageUrls);

                        if (lat != 0 && lng != 0) {
                            LatLng latLng = new LatLng(lat, lng);
                            mainHandler.post(() -> {
                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(name));
                                if (marker != null) {
                                    marker.setTag(detail);
                                }
                            });
                        }
                    }

                } catch (Exception e) {
                    Log.e("JSON_ERROR", "Parsing failed: " + e.getMessage());
                }
            }
        });
    }
}