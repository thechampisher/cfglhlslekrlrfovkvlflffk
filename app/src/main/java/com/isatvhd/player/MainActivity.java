package com.isatvhd.player;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private GitHubSource github;
    private LinearLayout rows;
    private TextView status;
    private final List<Channel> all = new ArrayList<>();
    private String selected = "DOMINO";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        github = new GitHubSource(this);
        buildUi();
        showCache();
    }

    private int dp(float n) {
        return (int)(n * getResources().getDisplayMetrics().density + .5f);
    }

    private TextView label(String text, int size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        return t;
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(dp(18), dp(16), dp(18), dp(10));
        root.setBackgroundColor(0xff0b1425);

        TextView title = label("İSA TV HD", 30);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = label("TELEVİZYON · FİLM · DİZİ", 12);
        root.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);

        MaterialButton domino = new MaterialButton(this);
        domino.setText("İSA TV HD 1\nDOMİNO");

        MaterialButton gunes = new MaterialButton(this);
        gunes.setText("İSA TV HD 2\nGÜNEŞ");

        tabs.addView(domino, new LinearLayout.LayoutParams(0, dp(62), 1));
        tabs.addView(gunes, new LinearLayout.LayoutParams(0, dp(62), 1));
        root.addView(tabs);

        MaterialButton scan = new MaterialButton(this);
        scan.setText("KAYNAKLARI GÜNCELLE");
        root.addView(scan);

        status = label("Hazır · Bir kaynak seçin.", 14);
        status.setPadding(0, dp(10), 0, dp(10));
        root.addView(status);

        ScrollView sc = new ScrollView(this);
        rows = new LinearLayout(this);
        rows.setOrientation(1);
        sc.addView(rows);
        root.addView(sc, new LinearLayout.LayoutParams(-1, 0, 1));

        setContentView(root);

        domino.setOnClickListener(v -> {
            selected = "DOMINO";
            render();
        });

        gunes.setOnClickListener(v -> {
            selected = "GUNES";
            render();
        });

        scan.setOnClickListener(v -> scan());
    }

    private void showCache() {
        all.clear();

        loadDominoCache();
        loadGunesCache();

        render();
    }

    private void loadDominoCache() {
        String s = github.readCache("DOMINO", "liste.json");
        if (s == null) return;

        try {
            all.addAll(JsonChannelParser.parse(s, "DOMINO", false));
        } catch (Exception ignored) {
        }
    }

    private void loadGunesCache() {
        for (String file : new String[]{"live2.json", "vod2.json"}) {
            String s = github.readCache("GUNES", file);
            if (s == null) continue;

            try {
                all.addAll(JsonChannelParser.parse(
                    s, "GUNES", file.equals("vod2.json")
                ));
            } catch (Exception ignored) {
            }
        }
    }

    private void scan() {
        status.setText("Kaynaklar ayrı ayrı güncelleniyor...");

        io.execute(() -> {
            List<Channel> dominoChannels = new ArrayList<>();
            List<Channel> gunesChannels = new ArrayList<>();

            String dominoMessage;
            String gunesMessage;

            // DOMİNO: Başarısız olursa GÜNEŞ güncellemesi devam eder.
            try {
                List<GitHubSource.Downloaded> files =
                    github.downloadRepo(
                        GitHubSource.DOMINO_OWNER,
                        GitHubSource.DOMINO_REPO,
                        Collections.singleton("liste.json")
                    );

                for (GitHubSource.Downloaded f : files) {
                    dominoChannels.addAll(
                        JsonChannelParser.parse(f.json, "DOMINO", false)
                    );
                }

                if (dominoChannels.isEmpty()) {
                    throw new Exception("Kanal bulunamadı veya veri biçimi desteklenmiyor.");
                }

                github.commitCache("DOMINO", files);
                dominoMessage = "Domino: " + dominoChannels.size() + " kayıt";
            } catch (Exception e) {
                dominoMessage = "Domino güncellenemedi: " + e.getMessage();
                dominoChannels.clear();
            }

            // GÜNEŞ: Domino'dan bağımsız indirilir ve çözülür.
            try {
                List<GitHubSource.Downloaded> files =
                    github.downloadRepo(
                        GitHubSource.GUNES_OWNER,
                        GitHubSource.GUNES_REPO,
                        new HashSet<>(Arrays.asList("live2.json", "vod2.json"))
                    );

                GunesPlaylistResolver resolver = new GunesPlaylistResolver();

                for (GitHubSource.Downloaded f : files) {
                    boolean vod = f.fileName.toLowerCase(Locale.ROOT)
                        .endsWith("vod2.json");

                    gunesChannels.addAll(resolver.resolve(f.json, vod));
                }

                if (gunesChannels.isEmpty()) {
                    throw new Exception("Çözümlenebilir yayın bulunamadı.");
                }

                github.commitCache("GUNES", files);
                gunesMessage = "Güneş: " + gunesChannels.size() + " kayıt";
            } catch (Exception e) {
                gunesMessage = "Güneş güncellenemedi: " + e.getMessage();
                gunesChannels.clear();
            }

            final String result =
                dominoMessage + "\n" + gunesMessage;

            runOnUiThread(() -> {
                all.clear();

                // Başarısız kaynağın önceki önbelleğini yükle.
                if (dominoChannels.isEmpty()) {
                    loadDominoCache();
                } else {
                    all.addAll(dominoChannels);
                }

                if (gunesChannels.isEmpty()) {
                    loadGunesCache();
                } else {
                    all.addAll(gunesChannels);
                }

                status.setText(result);
                render();
            });
        });
    }

    private void render() {
        rows.removeAllViews();
        int count = 0;

        for (Channel c : all) {
            if (selected.equals("DOMINO") &&
                !c.source.toUpperCase(Locale.ROOT).contains("DOMINO")) {
                continue;
            }

            if (selected.equals("GUNES") &&
                !c.source.toUpperCase(Locale.ROOT).contains("GUNES")) {
                continue;
            }

            TextView row = label(
                (c.isVod ? "▶  " : "●  ") + c.name +
                (c.group.isEmpty() ? "" : "\n" + c.group), 17
            );

            row.setPadding(dp(14), dp(14), dp(14), dp(14));
            row.setFocusable(true);
            row.setBackgroundResource(
                android.R.drawable.list_selector_background
            );

            row.setOnClickListener(v -> {
                Intent i = new Intent(this, PlayerActivity.class);
                i.putExtra("url", c.url);
                i.putExtra("name", c.name);
                startActivity(i);
            });

            rows.addView(row);
            count++;
        }

        if (count == 0) {
            TextView empty = label(
                "Bu kaynakta henüz gösterilecek kayıt yok.", 16
            );
            empty.setPadding(dp(12), dp(22), dp(12), dp(22));
            rows.addView(empty);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        io.shutdownNow();
    }
}
