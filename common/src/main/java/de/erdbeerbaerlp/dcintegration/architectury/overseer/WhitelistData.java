package de.erdbeerbaerlp.dcintegration.architectury.overseer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.io.*;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class WhitelistData {

    private static final Path FILE_PATH = Paths.get("TheOverseer-Data/whitelist_data.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static Map<String, MinecraftInfo> data = new ConcurrentHashMap<>();

    public record MinecraftInfo(String uuid, String name) {}

    // file operations
    public static void load() {
        if (!Files.exists(FILE_PATH)) return;

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Map<String, MinecraftInfo> loaded = GSON.fromJson(reader, new TypeToken<ConcurrentHashMap<String, MinecraftInfo>>(){}.getType());
            if (loaded != null) {
                data.putAll(loaded);
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to load whitelist data", e);
        }
    }

    public static void save() {
        CompletableFuture.runAsync(() -> {
            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(data, writer);
            } catch (IOException e) {
                DiscordIntegration.LOGGER.error("Failed to save whitelist data", e);
            }
        });
    }

    // data access
    public static void register(String discordId, String uuid, String name) {
        data.put(discordId, new MinecraftInfo(uuid, name));
        save();
    }

    public static MinecraftInfo get(String discordId) {
        return data.get(discordId);
    }

    public static void remove(String discordId) {
        data.remove(discordId);
        save();
    }
}
