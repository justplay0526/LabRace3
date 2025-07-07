package com.example.lab10.service;

import android.app.IntentService;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class FavoriteResultService extends IntentService {
    public static final String ACTION_DONE = "com.example.lab10.FAVORITE_DONE";

    public FavoriteResultService() {
        super("FavoriteResultService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Thread.sleep(2000); // 模擬延遲查資料
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Intent doneIntent = new Intent(ACTION_DONE);
        doneIntent.putExtras(intent); // 把原本資料帶回來
        LocalBroadcastManager.getInstance(this).sendBroadcast(doneIntent);
    }
}
