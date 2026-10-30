package com.isatvhd.player;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.content.Intent;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    // Worker adresi admin paneli kurulunca buraya bağlanacak.
    private static final String API_URL = "";

    private LinearLayout root;
    private LinearLayout channelContainer;
    private TextView status;
    private ExecutorService executor;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        executor = Executors.newSingleThreadExecutor();

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22, 28, 22, 20);
        root.setBackgroundColor(Color.rgb(12, 18, 28));

        TextView title = new TextView(this);
        title.setText("İSA TV HD");
        title.setTextSize(29);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 20, 8, 24);
        root.addView(title);

        TextView section = new TextView(this);
        section.setText("CANLI YAYIN");
        section.setTextSize(18);
        section.setTextColor(Color.rgb(0, 210, 160));
        section.setPadding(4, 12, 4, 12);
        root.addView(section);

        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(15);
        status.setPadding(4, 12, 4, 16);
        root.addView(status);

        Button refresh = new Button(this);
        refresh.setText("KANALLARI YENİLE");
        root.addView(refresh);

        ScrollView scroll = new ScrollView(this);
        channelContainer = new LinearLayout(this);
        channelContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(channelContainer);
        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1
        ));

        refresh.setOnClickListener(v -> loadChannels());

        setContentView(root);
        loadChannels();
    }

    private void loadChannels() {
        channelContainer.removeAllViews();

        if (API_URL.isEmpty()) {
            status.setText("Kanal sunucusu henüz bağlanmadı. Admin paneli kurulduğunda kanallar burada görünecek.");
            return;
        }

        status.setText("Kanallar yükleniyor...");

        executor.execute(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");

                StringBuilder data = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        data.append(line);
                    }
                }
                connection.disconnect();

                JSONObject json = new JSONObject(data.toString());
                JSONArray channels = json.optJSONArray("channels");

                runOnUiThread(() -> showChannels(channels));

            } catch (Exception e) {
                runOnUiThread(() ->
                        status.setText("Kanal listesi alınamadı: " + e.getMessage()));
            }
        });
    }

    private void showChannels(JSONArray channels) {
        channelContainer.removeAllViews();

        if (channels == null || channels.length() == 0) {
            status.setText("Henüz kanal eklenmemiş.");
            return;
        }

        status.setText(channels.length() + " kanal bulundu.");

        for (int i = 0; i < channels.length(); i++) {
            JSONObject channel = channels.optJSONObject(i);
            if (channel == null) continue;

            String name = channel.optString("name", "İsimsiz kanal");
            String stream = channel.optString("url", "");

            if (stream.isEmpty()) continue;

            Button item = new Button(this);
            item.setText(name);
            item.setAllCaps(false);
            channelContainer.addView(item);

            item.setOnClickListener(v -> {
                Intent intent = new Intent(this, PlayerActivity.class);
                intent.putExtra("url", stream);
                intent.putExtra("name", name);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) executor.shutdownNow();
    }
}
