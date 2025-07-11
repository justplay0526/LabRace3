package com.example.lab10.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.room.Room;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.example.lab10.data.AppDatabase;
import com.example.lab10.data.FavoriteSpot;
import com.example.lab10.R;
import com.example.lab10.data.SpotDetail;
import com.example.lab10.databinding.ActivityMainBinding;
import com.example.lab10.databinding.CustomDialogBinding;
import com.example.lab10.service.FavoriteResultService;
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
import java.util.Collections;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private ActivityMainBinding binding;
    private AppDatabase db;
    private GoogleMap mMap;
    private final List<Marker> markerList = new ArrayList<>();
    private final OkHttpClient client = new OkHttpClient();

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            String name = intent.getStringExtra("name");
            Toast.makeText(context, "已接收到「"+ name +"」的資料", Toast.LENGTH_SHORT).show();

            if (mMap != null) {
                LatLng location = new LatLng(lat, lng);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 16f));

                for (Marker marker : markerList) {
                    LatLng pos = marker.getPosition();
                    if (Math.abs(pos.latitude - location.latitude) < 0.0001 &&
                            Math.abs(pos.longitude - location.longitude) < 0.0001) {

                        // 找到對應 marker，移動畫面並顯示 title
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15));
                        marker.showInfoWindow();
                        break;
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spot-db").build();

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(FavoriteResultService.ACTION_DONE));

        getPermissionsThenInit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
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

    private void getPermissionsThenInit() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        else {
            initMap();
            binding.btnToFavorite.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, FavoriteActivity.class);
                startActivityForResult(intent, 100);
            });
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
                Manifest.permission.ACCESS_FINE_LOCATION
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
                        new LatLng(25.043, 121.535), 14));

        mMap.setOnMarkerClickListener(marker -> {
            showCustomDialog(marker);
            return true;
        });


        // 抓 API 並加上標記
        fetchAttractionsAndAddMarkers();
    }

    private void showCustomDialog(Marker marker) {
        CustomDialogBinding dialogBinding = CustomDialogBinding.inflate(getLayoutInflater());

        String name = marker.getTitle();
        LatLng position = marker.getPosition();

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .create();

        SpotDetail spotDetail = (SpotDetail) marker.getTag();
        if (spotDetail != null) {
            dialogBinding.tvPlaceName.setText(String.format("%s\n%s", spotDetail.name, spotDetail.address));
        } else {
            // fallback，如果沒資料就用 marker.title
            dialogBinding.tvPlaceName.setText(marker.getTitle());
        }

        dialogBinding.btnDetail.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra("name", spotDetail != null ? spotDetail.name : "");
            intent.putExtra("address", spotDetail != null ? spotDetail.address : "");
            intent.putExtra("intro", spotDetail != null ? spotDetail.introduction : "");
            intent.putStringArrayListExtra("images", new ArrayList<>(spotDetail != null ? spotDetail.imageUrls : Collections.emptyList()));
            startActivity(intent);
        });

        // 在 Dialog 開啟時先檢查是否已收藏
        new Thread(() -> {
            FavoriteSpot existing = db.favoriteDao().findByNameAndLatLng(name, position.latitude, position.longitude);

            runOnUiThread(() -> {
                if (existing != null) {
                    dialogBinding.btnFavorite.setText("已收藏");
                    dialogBinding.btnFavorite.setEnabled(false);
                }

                dialogBinding.btnFavorite.setOnClickListener(v -> {
                    // 再次防呆確認（避免 race condition）
                    new Thread(() -> {
                        FavoriteSpot checkAgain = db.favoriteDao().findByNameAndLatLng(name, position.latitude, position.longitude);
                        if (checkAgain == null) {
                            db.favoriteDao().insert(new FavoriteSpot(name, spotDetail != null ? spotDetail.address : "", position.latitude, position.longitude));

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
                .url("https://www.travel.taipei/open-api/zh-tw/Attractions/All?nlat=25.043148&elong=121.535816&page=1")
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

                        JSONArray fileArray = item.optJSONArray("images");
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
                                markerList.add(marker);
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