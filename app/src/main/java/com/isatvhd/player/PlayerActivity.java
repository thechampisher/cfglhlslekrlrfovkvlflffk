package com.isatvhd.player;

import android.app.Activity;
import android.widget.ImageView;
import android.graphics.BitmapFactory;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlayerActivity extends Activity {

    private ExoPlayer player;
    private PlayerView playerView;
    private TextView status;
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());

    private String streamUrl;
    private String channelName;
    private Map<String, String> headers = new HashMap<>();

    private static final int MAX_REDIRECTS = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        streamUrl = getIntent().getStringExtra("stream_url");
        channelName = getIntent().getStringExtra("channel_name");

        if (channelName == null) channelName = "İSA TV HD";

        if (streamUrl == null || streamUrl.trim().isEmpty()) {
            Toast.makeText(this, "Yayın adresi bulunamadı.",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String ua = getIntent().getStringExtra("user_agent");
        String referer = getIntent().getStringExtra("referer");
        String origin = getIntent().getStringExtra("origin");

        if (ua != null && !ua.isEmpty())
            headers.put("User-Agent", ua);

        if (referer != null && !referer.isEmpty())
            headers.put("Referer", referer);

        if (origin != null && !origin.isEmpty())
            headers.put("Origin", origin);

        String headersJson = getIntent().getStringExtra("headers_json");
        if (headersJson != null) {
            try {
                JSONObject object = new JSONObject(headersJson);
                Iterator<String> keys = object.keys();

                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = object.optString(key, "");

                    if (!value.isEmpty()
                            && key.matches("(?i)user-agent|referer|origin")) {
                        headers.put(key, value);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        createInterface();
        resolveAndPlay();
    }

    private void createInterface() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        playerView = (PlayerView)
                getLayoutInflater().inflate(
                        R.layout.isa_player_view,
                        root,
                        false
                );
        playerView.setUseController(true);

        root.addView(playerView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        status = new TextView(this);
        status.setVisibility(TextView.GONE);
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(0x99000000);
        status.setPadding(24, 18, 24, 18);

        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(
                -2, -2, Gravity.CENTER);

        root.addView(status, sp);

        // İSA TV HD logosu: yalnızca yatay TV ve tablet ekranlarında.
        if (shouldShowTvLogo()) {
            try {
                ImageView logo = new ImageView(this);

                try (java.io.InputStream input =
                        getAssets().open("streamflix/assets/logo.png")) {
                    logo.setImageBitmap(BitmapFactory.decodeStream(input));
                }

                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                logo.setPadding(dp(5), dp(5), dp(5), dp(5));
                logo.setBackgroundColor(0x880F0F0F);
                logo.setContentDescription("İSA TV HD");

                FrameLayout.LayoutParams logoParams =
                        new FrameLayout.LayoutParams(
                                dp(145),
                                dp(100),
                                Gravity.TOP | Gravity.RIGHT
                        );

                logoParams.setMargins(0, dp(20), dp(25), 0);

                // En son eklendiği için oynatıcı katmanının üzerinde kalır.
                root.addView(logo, logoParams);

            } catch (Exception e) {
                android.util.Log.e(
                        "ISATV",
                        "Logo yüklenemedi",
                        e
                );
            }
        }

        setContentView(root);
        setTitle(channelName);
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private boolean shouldShowTvLogo() {
        Configuration config = getResources().getConfiguration();

        boolean landscape =
                config.orientation == Configuration.ORIENTATION_LANDSCAPE;

        boolean television =
                (config.uiMode & Configuration.UI_MODE_TYPE_MASK)
                        == Configuration.UI_MODE_TYPE_TELEVISION;

        boolean tablet =
                config.smallestScreenWidthDp >= 600;

        return landscape && (television || tablet);
    }

    private void setStatus(String message) {
        // İSA TV HD: bağlantı ve yönlendirme bilgileri kullanıcıya gösterilmez.
        handler.post(() -> {
            if (status != null) {
                status.setVisibility(TextView.GONE);
            }
        });
    }

    private String resolveRedirects(String address) throws Exception {

        URL current = new URL(address);

        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {

            String protocol = current.getProtocol();

            if (!protocol.equalsIgnoreCase("http") &&
                !protocol.equalsIgnoreCase("https")) {
                throw new Exception("Desteklenmeyen bağlantı türü.");
            }

            HttpURLConnection connection =
                    (HttpURLConnection) current.openConnection();

            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Range", "bytes=0-0");
            connection.setRequestProperty("Accept", "*/*");

            for (Map.Entry<String, String> h : headers.entrySet()) {
                connection.setRequestProperty(h.getKey(), h.getValue());
            }

            int code = connection.getResponseCode();

            if (code == 301 || code == 302 ||
                code == 303 || code == 307 || code == 308) {

                String location = connection.getHeaderField("Location");
                connection.disconnect();

                if (location == null || location.trim().isEmpty()) {
                    throw new Exception("Yönlendirme adresi bulunamadı.");
                }

                if (hop == MAX_REDIRECTS) {
                    throw new Exception("8 yönlendirme sınırı aşıldı.");
                }

                current = new URL(current, location);

                final int count = hop + 1;
                setStatus("Yönlendirme takip ediliyor: " + count + "/8");

                continue;
            }

            if (code < 200 || code >= 400) {
                connection.disconnect();
                throw new Exception("Sunucu HTTP hatası: " + code);
            }

            String finalUrl = current.toExternalForm();
            connection.disconnect();

            return finalUrl;
        }

        throw new Exception("Son yayın adresine ulaşılamadı.");
    }

    private void resolveAndPlay() {

        setStatus("Yayın adresi çözülüyor...");

        executor.execute(() -> {
            try {
                String finalUrl = resolveRedirects(streamUrl);

                handler.post(() -> startPlayer(finalUrl));

            } catch (Exception e) {
                String error = e.getMessage();

                handler.post(() -> {
                    setStatus("Yayın açılamadı:\n" +
                            (error == null ? "Bilinmeyen hata" : error));

                });
            }
        });
    }

    private void startPlayer(String finalUrl) {

        try {
            setStatus("Oynatıcı hazırlanıyor...");

            DefaultHttpDataSource.Factory dataSourceFactory =
                    new DefaultHttpDataSource.Factory()
                    .setUserAgent(headers.containsKey("User-Agent")
                            ? headers.get("User-Agent")
                            : "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120 Safari/537.36")
                    .setConnectTimeoutMs(15000)
                    .setReadTimeoutMs(20000)
                    .setAllowCrossProtocolRedirects(true)
                    .setDefaultRequestProperties(headers);

            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);

            player.addListener(new Player.Listener() {

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_READY) {
                        status.setVisibility(TextView.GONE);
                    } else if (state == Player.STATE_BUFFERING) {
                        setStatus("Yayın yükleniyor...");
                    }
                }

                @Override
                public void onPlayerError(PlaybackException error) {
                    setStatus("Oynatma hatası: " +
                            error.getErrorCodeName());
                }
            });

            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(Uri.parse(finalUrl))
                    .build();

            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();

        } catch (Exception e) {
            setStatus("Oynatıcı başlatılamadı: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }

        executor.shutdownNow();
        super.onDestroy();
    }
}
