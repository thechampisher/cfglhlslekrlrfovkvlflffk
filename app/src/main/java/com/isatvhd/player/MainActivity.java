package com.isatvhd.player;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.graphics.Color;

import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_REQUEST = 2001;

    private final ExecutorService networkExecutor =
            Executors.newFixedThreadPool(3);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.BLACK);
        getWindow().setNavigationBarColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(12, 12, 12));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadsImagesAutomatically(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        webView.addJavascriptInterface(new ISATVBridge(), "ISATV");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                view.evaluateJavascript(
                    "(function(){" +
                    "document.querySelectorAll('.splash-screen,#splash-screen,.splash-overlay,.splash-loader')" +
                    ".forEach(function(e){e.style.display='none';});" +
                    "})();", null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                    WebView view,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {

                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                Intent intent = params.createIntent();

                try {
                    startActivityForResult(intent, FILE_REQUEST);
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }

                return true;
            }
        });

        setContentView(webView);
        webView.loadUrl("file:///android_asset/streamflix/index.html");
    }

    public class ISATVBridge {

        @JavascriptInterface
        public void play(String name, String url, String headersJson) {
            runOnUiThread(() -> {
                if (url == null || !url.matches("(?i)^https?://.*")) {
                    return;
                }

                Intent intent = new Intent(MainActivity.this, PlayerActivity.class);
                intent.putExtra("stream_url", url);
                intent.putExtra("channel_name",
                        name == null ? "İSA TV HD" : name);
                intent.putExtra("headers_json",
                        headersJson == null ? "{}" : headersJson);

                startActivity(intent);
            });
        }

        @JavascriptInterface
        public void fetchText(String address, String callbackId) {
            networkExecutor.execute(() -> {
                String result = null;
                String error = null;
                HttpURLConnection connection = null;

                try {
                    URL url = new URL(address);

                    if (!url.getProtocol().equalsIgnoreCase("http")
                            && !url.getProtocol().equalsIgnoreCase("https")) {
                        throw new Exception("Yalnızca HTTP/HTTPS adresleri desteklenir.");
                    }

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(15000);
                    connection.setReadTimeout(20000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
                    );

                    int code = connection.getResponseCode();

                    if (code < 200 || code >= 300) {
                        throw new Exception("HTTP " + code);
                    }

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8"));

                    StringBuilder output = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        output.append(line).append('\n');

                        if (output.length() > 15 * 1024 * 1024) {
                            throw new Exception("Liste boyutu 15 MB sınırını aştı.");
                        }
                    }

                    reader.close();
                    result = output.toString();

                } catch (Exception e) {
                    error = e.getMessage();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

                final String response = result;
                final String failure = error;

                runOnUiThread(() -> {
                    if (webView == null) return;

                    String safeId = callbackId.replaceAll("[^0-9]", "");

                    String js = "window.__isaFetchDone("
                            + safeId + ","
                            + JSONObject.quote(response)
                            + ","
                            + JSONObject.quote(failure)
                            + ");";

                    webView.evaluateJavascript(js, null);
                });
            });
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_REQUEST && fileCallback != null) {
            Uri[] result = WebChromeClient.FileChooserParams
                    .parseResult(resultCode, data);

            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }

        networkExecutor.shutdownNow();

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }
}
