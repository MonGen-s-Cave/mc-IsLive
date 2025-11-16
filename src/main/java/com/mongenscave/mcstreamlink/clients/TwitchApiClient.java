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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TwitchApiClient {
    private static final String API_BASE = "https://api.twitch.tv/helix";
    private static final String TOKEN_URL = "https://id.twitch.tv/oauth2/token";
    private static final Pattern USERNAME_PATTERN = Pattern.compile("twitch\\.tv/([^/?]+)");

    private final HttpClient httpClient;
    private final String clientId;
    private final String clientSecret;
    private final McStreamLink plugin;

    private String accessToken;
    private Instant tokenExpiry;

    public TwitchApiClient(@NotNull McStreamLink plugin, @NotNull String clientId, @NotNull String clientSecret) {
        this.plugin = plugin;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    private CompletableFuture<Boolean> ensureAccessToken() {
        if (accessToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) return CompletableFuture.completedFuture(true);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = String.format("%s?client_id=%s&client_secret=%s&grant_type=client_credentials",
                        TOKEN_URL, clientId, clientSecret);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .POST(HttpRequest.BodyPublishers.noBody())
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    LoggerUtils.error("TwitchApiClient failed to send Twitch API response");
                    return false;
                }

                JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                accessToken = jsonResponse.get("access_token").getAsString();
                int expiresIn = jsonResponse.get("expires_in").getAsInt();
                tokenExpiry = Instant.now().plusSeconds(expiresIn - 300);

                return true;
            } catch (Exception exception) {
                LoggerUtils.error(exception.getMessage());
                return false;
            }
        });
    }

    @Nullable
    private String extractUsername(@NotNull String url) {
        Matcher matcher = USERNAME_PATTERN.matcher(url);

        if (matcher.find()) return matcher.group(1).toLowerCase();
        return null;
    }

    @NotNull
    public CompletableFuture<Boolean> isChannelLive(@NotNull String channelUrl) {
        return ensureAccessToken().thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String username = extractUsername(channelUrl);
                    if (username == null) {
                        LoggerUtils.error("Invalid Channel");
                        return false;
                    }

                    String userId = getUserId(username);
                    if (userId == null) {
                        LoggerUtils.error("Invalid User");
                        return false;
                    }

                    return checkIfUserIsLive(userId);
                } catch (Exception exception) {
                    LoggerUtils.error(exception.getMessage());
                    return false;
                }
            });
        });
    }

    @Nullable
    private String getUserId(@NotNull String username) throws IOException, InterruptedException {
        String url = String.format("%s/users?login=%s", API_BASE, username);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            LoggerUtils.error("Error! Not 200 code: " + response.statusCode());
            return null;
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray data = jsonResponse.getAsJsonArray("data");

        if (data != null && !data.isEmpty()) return data.get(0).getAsJsonObject().get("id").getAsString();

        return null;
    }

    private boolean checkIfUserIsLive(@NotNull String userId) throws IOException, InterruptedException {
        String url = String.format("%s/streams?user_id=%s", API_BASE, userId);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Client-ID", clientId)
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            plugin.getLogger().warning("Twitch API hiba (stream): " + response.statusCode());
            return false;
        }

        JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
        JsonArray data = jsonResponse.getAsJsonArray("data");

        return data != null && !data.isEmpty() && data.get(0).getAsJsonObject().get("type").getAsString().equals("live");
    }

    public CompletableFuture<Integer> getFollowerCount(@NotNull String channelUrl) {
        return ensureAccessToken().thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(0);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String username = extractUsername(channelUrl);
                    if (username == null) return 0;

                    String userId = getUserId(username);
                    if (userId == null) return 0;

                    String url = String.format("%s/channels/followers?broadcaster_id=%s", API_BASE, userId);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Client-ID", clientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) return 0;

                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    return jsonResponse.get("total").getAsInt();
                } catch (Exception exception) {
                    LoggerUtils.error(exception.getMessage());
                    return 0;
                }
            });
        });
    }

    public CompletableFuture<Integer> getCurrentViewerCount(@NotNull String channelUrl) {
        return ensureAccessToken().thenCompose(success -> {
            if (!success) return CompletableFuture.completedFuture(0);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String username = extractUsername(channelUrl);
                    if (username == null) return 0;

                    String userId = getUserId(username);
                    if (userId == null) return 0;

                    String url = String.format("%s/streams?user_id=%s", API_BASE, userId);

                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(url))
                            .header("Client-ID", clientId)
                            .header("Authorization", "Bearer " + accessToken)
                            .GET()
                            .build();

                    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() != 200) return 0;

                    JsonObject jsonResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                    JsonArray data = jsonResponse.getAsJsonArray("data");

                    if (data == null || data.isEmpty()) return 0;

                    return data.get(0).getAsJsonObject().get("viewer_count").getAsInt();
                } catch (Exception exception) {
                    LoggerUtils.error(exception.getMessage());
                    return 0;
                }
            });
        });
    }
}
