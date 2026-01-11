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
    // sync discord memeber list to see if anyone left/joined while the server was down
    public static boolean SYNC_ON_STARTUP = true;
    // if the user should get kicked from mc server after removal from whitelist
    public static boolean FORCE_KICK = true;
    public static String WHITELIST_CHANNEL_ID = "000000000000000000";
    public static String FORMER_MIRTHER_ROLE_ID = "000000000000000000";

    public static void load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                saveDefaults();
                return;
            }

            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                props.load(reader);

                ENABLED = Boolean.parseBoolean(props.getProperty("enabled", String.valueOf(ENABLED)));
                SYNC_ON_STARTUP = Boolean.parseBoolean(props.getProperty("sync_on_startup", String.valueOf(SYNC_ON_STARTUP)));
                FORCE_KICK = Boolean.parseBoolean(props.getProperty("force_kick", String.valueOf(FORCE_KICK)));
                WHITELIST_CHANNEL_ID = props.getProperty("whitelist_channel_id", WHITELIST_CHANNEL_ID);
                FORMER_MIRTHER_ROLE_ID = props.getProperty("former_mirther_role_id", FORMER_MIRTHER_ROLE_ID);

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
            props.setProperty("sync_on_startup", String.valueOf(SYNC_ON_STARTUP));
            props.setProperty("force_kick", String.valueOf(FORCE_KICK));
            props.setProperty("whitelist_channel_id", WHITELIST_CHANNEL_ID);
            props.setProperty("former_mirther_role_id", FORMER_MIRTHER_ROLE_ID);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                props.store(writer, "The Overseer Configuration File");
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to generate Overseer config", e);
        }
    }
}
