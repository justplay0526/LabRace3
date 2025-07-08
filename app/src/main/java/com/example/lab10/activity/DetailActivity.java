package com.example.lab10.activity;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.lab10.databinding.ActivityDetailBinding;

import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {
    private ActivityDetailBinding binding;
    ArrayList<String> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        String intro = getIntent().getStringExtra("intro");
        images = getIntent().getStringArrayListExtra("images");

        binding.tvName.setText(name);
        binding.tvAddress.setText(address);
        binding.tvIntro.setText(intro);
        if (images!= null && !images.isEmpty()) {
            Glide.with(this)
                    .load(images.get(0)) // 可以是 URL 或本地資源
                    .into(binding.ivImage);
        } else {
            binding.ivImage.setVisibility(ImageView.GONE);
            binding.tvNoImage.setText("資料來源未提供圖片");
        }
        binding.btnBack.setOnClickListener(v -> {
            finish(); // 回上一頁
        });
    }
}
