package de.erdbeerbaerlp.dcintegration.architectury.overseer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WhitelistData {

    private static final Path FILE_PATH = Paths.get("TheOverseer-Data/whitelist_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static Map<String, List<MinecraftInfo>> data = new ConcurrentHashMap<>();

    public record MinecraftInfo(String uuid, String name) {}

    public enum LinkResult {
        SUCCESS,
        ALREADY_OWNED,
        TAKEN_BY_OTHER
    }

    // file operations
    public static void load() {
        if (!Files.exists(FILE_PATH)) return;

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Map<String, List<MinecraftInfo>> loaded = GSON.fromJson(reader,
                    new TypeToken<ConcurrentHashMap<String, CopyOnWriteArrayList<MinecraftInfo>>>(){}.getType());
            if (loaded != null) {
                data.putAll(loaded);
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to load whitelist data", e);
        }
    }

    public static void save() {
        CompletableFuture.runAsync(() -> {
            try {
                if (FILE_PATH.getParent() != null) {
                    Files.createDirectories(FILE_PATH.getParent());
                }

                try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                    GSON.toJson(data, writer);
                }
            } catch (IOException e) {
                DiscordIntegration.LOGGER.error("Failed to save whitelist data", e);
            }
        });
    }

    // data access
    public static LinkResult register(String discordId, String uuid, String name) {
        for (Map.Entry<String, List<MinecraftInfo>> entry : data.entrySet()) {
            for (MinecraftInfo info : entry.getValue()) {
                if (info.uuid().equals(uuid)) {
                    if (entry.getKey().equals(discordId)) {
                        return LinkResult.ALREADY_OWNED;
                    } else {
                        return LinkResult.TAKEN_BY_OTHER;
                    }
                }
            }
        }

        data.computeIfAbsent(discordId, k -> new CopyOnWriteArrayList<>())
                .add(new MinecraftInfo(uuid, name));
        save();
        return LinkResult.SUCCESS;
    }

    public static List<MinecraftInfo> get(String discordId) {
        return data.get(discordId);
    }

    public static Set<String> getAllDiscordIds() {
        return data.keySet();
    }

    // mojang api
    public static CompletableFuture<MinecraftInfo> fetchFromMojang(String username) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + username))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        return HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        Map<String, String> map = GSON.fromJson(response.body(), new TypeToken<Map<String, String>>(){}.getType());
                        return new MinecraftInfo(map.get("id"), map.get("name"));
                    }
                    return null;
                })
                .exceptionally(ex -> {
                    DiscordIntegration.LOGGER.error("Error fetching Mojang profile for " + username, ex);
                    return null;
                });
    }

    static {
        load();
    }
}
