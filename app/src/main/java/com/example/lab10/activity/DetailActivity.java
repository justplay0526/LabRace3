package com.example.lab10.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lab10.R;

import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {
    TextView tvName, tvAddress, tvIntro, tvNoImage;
    ImageView ivImage;
    ArrayList<String> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        tvIntro = findViewById(R.id.tvIntro);
        tvNoImage = findViewById(R.id.tvNoImage);
        ivImage = findViewById(R.id.ivImage);

        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        String intro = getIntent().getStringExtra("intro");
        images = getIntent().getStringArrayListExtra("images");

        tvName.setText(name);
        tvAddress.setText(address);
        tvIntro.setText(intro);
        if (images!= null && !images.isEmpty()) {
            Glide.with(this)
                    .load(images.get(0)) // 可以是 URL 或本地資源
                    .into(ivImage);
        } else {
            ivImage.setVisibility(ImageView.GONE);
            tvNoImage.setText("資料來源未提供圖片");
        }
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish(); // 回上一頁
        });
    }
}
