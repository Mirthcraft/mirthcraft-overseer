package de.erdbeerbaerlp.dcintegration.architectury.overseer;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class OverseerConfig {
    private static final Path CONFIG_PATH = Paths.get("config/TheOverseer.properties");
    private static final Properties props = new Properties();

    public static boolean ENABLED = true;
    public static String WHITELIST_CHANNEL_ID = "000000000000000000";

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaults();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                props.load(reader);

                ENABLED = Boolean.parseBoolean(props.getProperty("enabled", String.valueOf(ENABLED)));
                WHITELIST_CHANNEL_ID = props.getProperty("whitelist_channel_id", WHITELIST_CHANNEL_ID);

                DiscordIntegration.LOGGER.info("The Overseer config loaded successfully");
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to load Overseer config", e);
        }
    }

    private static void saveDefaults() {
        try {
            if (CONFIG_PATH.getParent() != null) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }

            props.setProperty("enabled", String.valueOf(ENABLED));
            props.setProperty("whitelist_channel_id", WHITELIST_CHANNEL_ID);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                props.store(writer, "The Overseer Configuration File");
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to generate Overseer config", e);
        }
    }
}
