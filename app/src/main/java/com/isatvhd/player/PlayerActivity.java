package com.isatvhd.player;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.common.MediaItem;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends Activity {

    private ExoPlayer player;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        String url = getIntent().getStringExtra("url");
        String name = getIntent().getStringExtra("name");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);

        TextView title = new TextView(this);
        title.setText(name == null ? "Canlı Yayın" : name);
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setPadding(12, 10, 12, 10);
        layout.addView(title);

        PlayerView playerView = new PlayerView(this);
        layout.addView(playerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        setContentView(layout);

        if (url != null && !url.isEmpty()) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            player.setMediaItem(MediaItem.fromUri(url));
            player.prepare();
            player.play();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
