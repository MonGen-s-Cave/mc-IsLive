package com.mongenscave.mcstreamlink.clients;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongenscave.mcstreamlink.McStreamLink;
import com.mongenscave.mcstreamlink.utils.LoggerUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeApiClient {
    private static final String API_BASE = "https://www.googleapis.com/youtube/v3";
    private static final Pattern CHANNEL_ID_PATTERN = Pattern.compile("youtube\\.com/channel/([^/?]+)");
    private static final Pattern CHANNEL_USERNAME_PATTERN = Pattern.compile("youtube\\.com/@([^/?]+)");
    private static final Pattern CHANNEL_CUSTOM_PATTERN = Pattern.compile("youtube\\.com/c/([^/?]+)");
    private static final Pattern CHANNEL_USER_PATTERN = Pattern.compile("youtube\\.com/user/([^/?]+)");

    private final HttpClient httpClient;
    private final String apiKey;
    private final McStreamLink plugin;

    public YoutubeApiClient(@NotNull McStreamLink plugin, @NotNull String apiKey) {
        this.plugin = plugin;
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @NotNull
    public CompletableFuture<Boolean> isChannelLive(@NotNull String channelUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String channelId = extractChannelId(channelUrl);
                if (channelId == null) {
                    LoggerUtils.error("Failed to extract channel ID from URL: " + channelUrl);
                    return false;
                }

                return checkIfChannelIsLive(channelId);
            } catch (Exception exception) {
                LoggerUtils.error(exception.getMessage());
                return false;
            }
        });
    }

    @Nullable
    private String extractChannelId(@NotNull String url) throws IOException, InterruptedException {
        Matcher channelMatcher = CHANNEL_ID_PATTERN.matcher(url);
        if (channelMatcher.find()) return channelMatcher.group(1);

        Matcher usernameMatcher = CHANNEL_USERNAME_PATTERN.matcher(url);
        if (usernameMatcher.find()) return resolveChannelByHandle(usernameMatcher.group(1));

        Matcher customMatcher = CHANNEL_CUSTOM_PATTERN.matcher(url);
        if (customMatcher.find()) return resolveChannelByUsername(customMatcher.group(1));

        Matcher userMatcher = CHANNEL_USER_PATTERN.matcher(url);
        if (userMatcher.find()) return resolveChannelByUsername(userMatcher.group(1));

        return null;
    }

    @Nullable
    private String resolveChannelByHandle(@NotNull String handle) throws IOException, InterruptedException {
        String encodedHandle = URLEncoder.encode("@" + handle, StandardCharsets.UTF_8);
        String url = String.format("%s/channels?part=id&forHandle=%s&key=%s",
                API_BASE, encodedHandle, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            plugin.getLogger().warning("YouTube API hiba (handle): " + response.statusCode());
            return null;
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray items = jsonResponse.getAsJsonArray("items");

        if (items != null && !items.isEmpty()) return items.get(0).getAsJsonObject().get("id").getAsString();
        return null;
    }

    @Nullable
    private String resolveChannelByUsername(@NotNull String username) throws IOException, InterruptedException {
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String url = String.format("%s/channels?part=id&forUsername=%s&key=%s",
                API_BASE, encodedUsername, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            plugin.getLogger().warning("YouTube API hiba (username): " + response.statusCode());
            return null;
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray items = jsonResponse.getAsJsonArray("items");

        if (items != null && !items.isEmpty()) return items.get(0).getAsJsonObject().get("id").getAsString();
        return null;
    }

    private boolean checkIfChannelIsLive(@NotNull String channelId) throws IOException, InterruptedException {
        String url = String.format("%s/search?part=snippet&channelId=%s&eventType=live&type=video&key=%s",
                API_BASE, channelId, apiKey);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            plugin.getLogger().warning("YouTube API hiba (live check): " + response.statusCode() + " - " + response.body());
            return false;
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();

        if (jsonResponse.has("error")) {
            JsonObject error = jsonResponse.getAsJsonObject("error");
            plugin.getLogger().severe("YouTube API hiba: " + error.get("message").getAsString());
            return false;
        }

        JsonArray items = jsonResponse.getAsJsonArray("items");
        return items != null && !items.isEmpty();
    }

    public CompletableFuture<Integer> getSubscriberCount(@NotNull String channelUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String channelId = extractChannelId(channelUrl);
                if (channelId == null) return 0;

                String url = String.format("%s/channels?part=statistics&id=%s&key=%s",
                        API_BASE, channelId, apiKey);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) return 0;

                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items == null || items.isEmpty()) return 0;

                JsonObject statistics = items.get(0).getAsJsonObject()
                        .getAsJsonObject("statistics");

                return statistics.get("subscriberCount").getAsInt();
            } catch (Exception exception) {
                LoggerUtils.error(exception.getMessage());
                return 0;
            }
        });
    }

    public CompletableFuture<Integer> getCurrentViewerCount(@NotNull String channelUrl) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String channelId = extractChannelId(channelUrl);
                if (channelId == null) return 0;

                String url = String.format("%s/search?part=snippet&channelId=%s&eventType=live&type=video&key=%s",
                        API_BASE, channelId, apiKey);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) return 0;

                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = jsonResponse.getAsJsonArray("items");

                if (items == null || items.isEmpty()) return 0;

                String videoId = items.get(0).getAsJsonObject().getAsJsonObject("id")
                        .get("videoId").getAsString();

                String videoUrl = String.format("%s/videos?part=liveStreamingDetails&id=%s&key=%s",
                        API_BASE, videoId, apiKey);

                HttpRequest videoRequest = HttpRequest.newBuilder()
                        .uri(URI.create(videoUrl))
                        .GET()
                        .build();

                HttpResponse<String> videoResponse = httpClient.send(videoRequest, HttpResponse.BodyHandlers.ofString());

                if (videoResponse.statusCode() != 200) return 0;

                JsonObject videoJson = JsonParser.parseString(videoResponse.body()).getAsJsonObject();
                JsonArray videoItems = videoJson.getAsJsonArray("items");

                if (videoItems == null || videoItems.isEmpty()) return 0;

                JsonObject liveDetails = videoItems.get(0).getAsJsonObject()
                        .getAsJsonObject("liveStreamingDetails");

                if (liveDetails == null || !liveDetails.has("concurrentViewers")) return 0;

                return liveDetails.get("concurrentViewers").getAsInt();
            } catch (Exception exception) {
                LoggerUtils.error(exception.getMessage());
                return 0;
            }
        });
    }
}
