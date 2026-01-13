package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FriendFridayData {
    private static final Path FILE_PATH = Paths.get("TheOverseer-Data/friend_friday_invites.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Set<String> activeCodes = ConcurrentHashMap.newKeySet();

    public static void load() {
        if (!Files.exists(FILE_PATH)) return;
        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Set<String> loaded = GSON.fromJson(reader, new TypeToken<Set<String>>(){}.getType());
            if (loaded != null) {
                activeCodes.addAll(loaded);
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to load Friend Friday invites", e);
        }
    }

    public static void save() {
        CompletableFuture.runAsync(() ->{
            try {
                if (FILE_PATH.getParent() != null) {
                    Files.createDirectories(FILE_PATH.getParent());
                }
                try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                    GSON.toJson(activeCodes, writer);
                }
            } catch (IOException e) {
                DiscordIntegration.LOGGER.error("Failed to save Friend Friday invites", e);
            }
        });
    }

    public static void addCodes(String code) {
        activeCodes.add(code);
        save();
    }

    public static void removeCodes(String code) {
        activeCodes.remove(code);
        save();
    }

    public static boolean isFriendFridayCode(String code){
        return activeCodes.contains(code);
    }

    public static void clear() {
        activeCodes.clear();
        save();
    }
}
