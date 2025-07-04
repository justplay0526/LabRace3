package com.example.lab10;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
    private AppDatabase db;
    private RecyclerView recyclerView;
    private FavoriteAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoriteAdapter();
        recyclerView.setAdapter(adapter);

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spot-db").build();

        loadFavorites();

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish(); // 回上一頁
        });

        adapter.setOnItemClickListener(spot -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", spot.lat);
            resultIntent.putExtra("lng", spot.lng);
            resultIntent.putExtra("name", spot.name);
            setResult(RESULT_OK, resultIntent);
            finish(); // 結束頁面回 MainActivity
        });

        adapter.setOnItemLongClickListener(spot -> {
            new AlertDialog.Builder(this)
                    .setTitle("刪除收藏")
                    .setMessage("確定要刪除「" + spot.name + "」嗎？")
                    .setPositiveButton("刪除", (dialog, which) -> {
                        new Thread(() -> {
                            db.favoriteDao().delete(spot);
                            runOnUiThread(() -> {
                                Toast.makeText(this, "已刪除", Toast.LENGTH_SHORT).show();
                                loadFavorites(); // 重新載入清單
                            });
                        }).start();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<FavoriteSpot> spots = db.favoriteDao().getAll();
            runOnUiThread(() -> adapter.setData(spots));
        }).start();
    }
}