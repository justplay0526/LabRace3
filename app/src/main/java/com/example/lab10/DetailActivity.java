package com.example.lab10;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {
    TextView tvName, tvAddress, tvIntro;
    ArrayList<String> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        tvName = findViewById(R.id.tvName);
        tvAddress = findViewById(R.id.tvAddress);
        tvIntro = findViewById(R.id.tvIntro);

        String name = getIntent().getStringExtra("name");
        String address = getIntent().getStringExtra("address");
        String intro = getIntent().getStringExtra("intro");
        images = getIntent().getStringArrayListExtra("images");

        tvName.setText(name);
        tvAddress.setText(address);
        tvIntro.setText(intro);
    }
}
