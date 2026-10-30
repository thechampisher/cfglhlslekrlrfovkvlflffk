package com.isatvhd.player;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.ui.PlayerView;

/**
 * Video is decoded by the normal Android/Media3 video renderer.
 * If the optional official Media3 FFmpeg audio AAR is installed in app/libs,
 * its FfmpegAudioRenderer is automatically discovered and preferred for audio.
 */
public class PlayerActivity extends AppCompatActivity {
 private ExoPlayer player;
 @Override protected void onCreate(Bundle b) {
  super.onCreate(b);
  String url=getIntent().getStringExtra("url");
  String name=getIntent().getStringExtra("name");
  LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.BLACK);
  TextView title=new TextView(this);
  title.setText(name==null?"İSA TV HD":name);
  title.setTextColor(Color.WHITE);
  title.setTextSize(18);
  root.addView(title);
  PlayerView view=new PlayerView(this);
  root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
  setContentView(root);

  DefaultRenderersFactory renderers = new DefaultRenderersFactory(this)
      .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);
  player=new ExoPlayer.Builder(this,renderers).build();
  view.setPlayer(player);
  if(url!=null&&!url.isEmpty()) {
   player.setMediaItem(MediaItem.fromUri(url));
   player.prepare();
   player.play();
  }
 }
 @Override protected void onStop() {
  super.onStop();
  if(player!=null){player.release();player=null;}
 }
}
