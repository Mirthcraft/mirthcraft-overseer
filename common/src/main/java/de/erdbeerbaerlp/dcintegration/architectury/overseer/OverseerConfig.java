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
    public static String GUILD_ID = "000000000000000000";
    public static String WHITELIST_CHANNEL_ID = "000000000000000000";
    public static String ACTIVE_MIRTHER_ROLE_ID = "000000000000000000";
    public static String FORMER_MIRTHER_ROLE_ID = "000000000000000000";

    // Friend Friday Config
    public static boolean FF_ENABLED = true;
    public static int FF_END_HOUR_UTC = 7;
    public static String FF_END_DAY = "SATURDAY";
    public static String FF_ANNOUNCE_CHANNEL_ID = "000000000000000000";
    public static String FF_PING_ROLE_ID = "000000000000000000";
    public static String FF_ROLE_ID = "000000000000000000";
    public static String FF_KICK_DM = "Sadly Friend Friday is over! Thank you for joining us and we hope to see you again. If you wish to stay, keep a look out for applications to open!";

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
                GUILD_ID = props.getProperty("guild_id", GUILD_ID);
                WHITELIST_CHANNEL_ID = props.getProperty("whitelist_channel_id", WHITELIST_CHANNEL_ID);
                ACTIVE_MIRTHER_ROLE_ID = props.getProperty("active_mirther_role_id", ACTIVE_MIRTHER_ROLE_ID);
                FORMER_MIRTHER_ROLE_ID = props.getProperty("former_mirther_role_id", FORMER_MIRTHER_ROLE_ID);

                // load Friend Friday
                FF_ENABLED = Boolean.parseBoolean(props.getProperty("ff_enabled", String.valueOf(FF_ENABLED)));
                FF_END_HOUR_UTC = Integer.parseInt(props.getProperty("ff_end_hour_utc", String.valueOf(FF_END_HOUR_UTC)));
                FF_END_DAY = props.getProperty("ff_end_day", FF_END_DAY);
                FF_ANNOUNCE_CHANNEL_ID = props.getProperty("ff_announce_channel_id", FF_ANNOUNCE_CHANNEL_ID);
                FF_PING_ROLE_ID = props.getProperty("ff_ping_role_id", FF_PING_ROLE_ID);
                FF_ROLE_ID = props.getProperty("ff_role_id", FF_ROLE_ID);
                FF_KICK_DM = props.getProperty("ff_kick_dm", FF_KICK_DM);

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
            props.setProperty("guild_id", GUILD_ID);
            props.setProperty("whitelist_channel_id", WHITELIST_CHANNEL_ID);
            props.setProperty("active_mirther_role_id", ACTIVE_MIRTHER_ROLE_ID);
            props.setProperty("former_mirther_role_id", FORMER_MIRTHER_ROLE_ID);

            // friend friday
            props.setProperty("ff_enabled", String.valueOf(FF_ENABLED));
            props.setProperty("ff_end_hour_utc", String.valueOf(FF_END_HOUR_UTC));
            props.setProperty("ff_end_day", FF_END_DAY);
            props.setProperty("ff_announce_channel_id", FF_ANNOUNCE_CHANNEL_ID);
            props.setProperty("ff_ping_role_id", FF_PING_ROLE_ID);
            props.setProperty("ff_role_id", FF_ROLE_ID);
            props.setProperty("ff_kick_dm", FF_KICK_DM);

            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                props.store(writer, "The Overseer Configuration File");
            }
        } catch (IOException e) {
            DiscordIntegration.LOGGER.error("Failed to generate Overseer config", e);
        }
    }
}
