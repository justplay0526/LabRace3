package com.example.lab10;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
    private AppDatabase db;
    private FavoriteAdapter adapter;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // 接收資料後回傳給 MainActivity
            double lat = intent.getDoubleExtra("lat", 0);
            double lng = intent.getDoubleExtra("lng", 0);
            String name = intent.getStringExtra("name");
            Toast.makeText(context, "已接收到「"+ name +"」的資料", Toast.LENGTH_SHORT).show();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("lat", lat);
            resultIntent.putExtra("lng", lng);
            resultIntent.putExtra("name", name);
            FavoriteActivity.this.setResult(RESULT_OK, resultIntent);
            finish();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FavoriteAdapter();
        recyclerView.setAdapter(adapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver,
                new IntentFilter(FavoriteResultService.ACTION_DONE));

        db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "spot-db").build();

        loadFavorites();

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish(); // 回上一頁
        });

        adapter.setOnItemClickListener(spot -> {
            Toast.makeText(this, "正在讀取「" + spot.name + "」相關資料", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(this, FavoriteResultService.class);
            intent.putExtra("lat", spot.lat);
            intent.putExtra("lng", spot.lng);
            intent.putExtra("name", spot.name);
            startService(intent); // 啟動 Service 模擬延遲處理
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void loadFavorites() {
        new Thread(() -> {
            List<FavoriteSpot> spots = db.favoriteDao().getAll();
            runOnUiThread(() -> adapter.setData(spots));
        }).start();
    }
}