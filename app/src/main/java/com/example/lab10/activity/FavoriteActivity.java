package com.example.lab10.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import com.example.lab10.data.AppDatabase;
import com.example.lab10.adapter.FavoriteAdapter;
import com.example.lab10.databinding.ActivityFavoriteBinding;
import com.example.lab10.service.FavoriteResultService;
import com.example.lab10.data.FavoriteSpot;

import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
    private ActivityFavoriteBinding binding;
    private AppDatabase db;
    private FavoriteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFavoriteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initRecyclerView();

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spot-db").build();

        loadFavorites();
        setListener();
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<FavoriteSpot> spots = db.favoriteDao().getAll();
            runOnUiThread(() -> adapter.setData(spots));
        }).start();
    }

    private void initRecyclerView() {
        adapter = new FavoriteAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setListener() {
        binding.btnBack.setOnClickListener(v -> {
            finish(); // 回上一頁
        });

        adapter.setOnItemClickListener(spot -> {
            Toast.makeText(this, "正在讀取「" + spot.name + "」相關資料", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, FavoriteResultService.class);
            intent.putExtra("lat", spot.lat);
            intent.putExtra("lng", spot.lng);
            intent.putExtra("name", spot.name);
            startService(intent); // 啟動 Service 模擬延遲處理
            finish();
        });

        adapter.setOnItemLongClickListener(spot -> new AlertDialog.Builder(this)
                .setTitle("刪除收藏")
                .setMessage("確定要刪除「" + spot.name + "」嗎？")
                .setPositiveButton("刪除", (dialog, which) -> new Thread(() -> {
                    db.favoriteDao().delete(spot);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "已刪除", Toast.LENGTH_SHORT).show();
                        loadFavorites(); // 重新載入清單
                    });
                }).start())
                .setNegativeButton("取消", null)
                .show());
    }
}