package com.isatvhd.player;

import com.google.gson.*;
import okhttp3.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GunesPlaylistResolver {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .followRedirects(true)
            .build();

    private final Set<String> visited = new HashSet<>();
    private final List<Channel> channels = new ArrayList<>();

    public List<Channel> resolve(String json, boolean vod) throws Exception {
        visited.clear();
        channels.clear();

        JsonElement root = JsonParser.parseString(json);
        walk(root, "GÜNEŞ", vod, 0);

        return new ArrayList<>(channels);
    }

    private void walk(JsonElement element, String group,
                      boolean vod, int depth) throws Exception {

        if (element == null || element.isJsonNull() || depth > 5)
            return;

        if (element.isJsonArray()) {
            for (JsonElement item : element.getAsJsonArray())
                walk(item, group, vod, depth);
            return;
        }

        if (!element.isJsonObject())
            return;

        JsonObject object = element.getAsJsonObject();

        String name = value(object, "name", "title", "isim", "channel");
        String logo = value(object, "poster", "logo", "image", "icon");
        String category = value(object, "group", "category", "kategori");
        String url = value(object, "url", "link", "stream", "source");

        if (!url.isEmpty()) {
            String content = downloadIfPlaylist(url);

            if (content != null) {
                parsePlaylist(content, url, name, logo, category, vod, depth + 1);
            } else if (isPlayable(url)) {
                add(name, url, logo, category, vod);
            }
        }

        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            JsonElement child = entry.getValue();

            if (child.isJsonObject() || child.isJsonArray())
                walk(child, category.isEmpty() ? group : category,
                        vod, depth + 1);
        }
    }

    private String downloadIfPlaylist(String url) {
        if (!visited.add(url) || visited.size() > 300)
            return null;

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "ISA-TV-HD/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null)
                    return null;

                String body = response.body().string();
                String trimmed = body.trim();

                if (trimmed.startsWith("#EXTM3U") ||
                    trimmed.startsWith("{") ||
                    trimmed.startsWith("["))
                    return body;
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void parsePlaylist(String body, String baseUrl,
                               String parentName, String logo,
                               String category, boolean vod,
                               int depth) throws Exception {

        if (depth > 5) return;

        String text = body.trim();

        if (text.startsWith("{") || text.startsWith("[")) {
            walk(JsonParser.parseString(text),
                    category.isEmpty() ? "GÜNEŞ" : category,
                    vod, depth);
            return;
        }

        String currentName = parentName;
        String currentLogo = logo;
        String currentGroup = category;

        for (String raw : body.split("\\r?\\n")) {
            String line = raw.trim();

            if (line.isEmpty()) continue;

            if (line.startsWith("#EXTINF:")) {
                int comma = line.lastIndexOf(',');
                if (comma >= 0)
                    currentName = line.substring(comma + 1).trim();

                currentLogo = attribute(line, "tvg-logo", currentLogo);
                currentGroup = attribute(line, "group-title", currentGroup);
                continue;
            }

            if (line.startsWith("#")) continue;

            String resolved = resolveUrl(baseUrl, line);

            String nested = downloadIfPlaylist(resolved);

            if (nested != null) {
                parsePlaylist(nested, resolved, currentName,
                        currentLogo, currentGroup, vod, depth + 1);
            } else if (isPlayable(resolved)) {
                add(currentName, resolved, currentLogo, currentGroup, vod);
            }
        }
    }

    private String resolveUrl(String base, String path) {
        try {
            return java.net.URI.create(base).resolve(path).toString();
        } catch (Exception e) {
            return path;
        }
    }

    private String attribute(String line, String key, String fallback) {
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile(
                        key + "=\"([^\"]*)\"",
                        java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(line);

        return matcher.find() ? matcher.group(1) : fallback;
    }

    private String value(JsonObject object, String... keys) {
        for (String key : keys) {
            if (!object.has(key) || object.get(key).isJsonNull())
                continue;

            JsonElement value = object.get(key);

            if (value.isJsonPrimitive()) {
                String result = value.getAsString().trim();
                if (!result.isEmpty()) return result;
            }
        }
        return "";
    }

    private boolean isPlayable(String url) {
        String value = url.toLowerCase(Locale.ROOT);

        return value.startsWith("http://") ||
               value.startsWith("https://") ||
               value.startsWith("rtmp://") ||
               value.startsWith("rtsp://");
    }

    private void add(String name, String url, String logo,
                     String group, boolean vod) {

        if (!isPlayable(url)) return;

        for (Channel channel : channels) {
            if (channel.url.equals(url)) return;
        }

        if (name == null || name.trim().isEmpty())
            name = "İsimsiz yayın";

        channels.add(new Channel(
                name, url, logo == null ? "" : logo,
                group == null ? "" : group,
                "GUNES", vod
        ));
    }
}
