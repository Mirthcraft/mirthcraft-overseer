package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class FriendFridayData {
    private static final Path FILE_PATH = Paths.get("TheOverseer-Data/friend_friday_invites.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static long endTime = -1;
    public static String currentInvite = "";

    public static void load() {
        if (!Files.exists(FILE_PATH)) return;
        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            if (json != null) {
                if (json.has("endTime")) endTime = json.get("endTime").getAsLong();
                if (json.has("currentInvite")) currentInvite = json.get("currentInvite").getAsString();
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to load Friend Friday data", e);
        }
    }

    public static void save() {
        CompletableFuture.runAsync(() ->{
            try {
                if (FILE_PATH.getParent() != null) {
                    Files.createDirectories(FILE_PATH.getParent());
                }

                JsonObject json = new JsonObject();
                json.addProperty("endTime", endTime);
                json.addProperty("currentInvite", currentInvite);

                try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                    GSON.toJson(json, writer);
                }
            } catch (IOException e) {
                DiscordIntegration.LOGGER.error("Failed to save Friend Friday data", e);
            }
        });
    }

    public static boolean isActive() {
        return endTime > System.currentTimeMillis();
    }

    public static void clear() {
        endTime = -1;
        currentInvite = "";
        save();
    }
}
