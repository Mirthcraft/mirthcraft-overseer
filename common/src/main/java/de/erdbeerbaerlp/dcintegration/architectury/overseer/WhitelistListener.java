package de.erdbeerbaerlp.dcintegration.architectury.overseer;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class WhitelistListener extends ListenerAdapter {

    // i know these should NOT be hardcoded but I cant be bothered to set up a config rn
    private static final String WHITELIST_ROLE_ID = "nothing";
    private static final String WHITELIST_CHANNEL_ID = "1458612571794964503";

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {

        if (!event.getChannel().getId().equals(WHITELIST_CHANNEL_ID)) return;
        if (event.getAuthor().isBot()) return;

        String ign = event.getMessage().getContentRaw().trim();

        // ignore sentence
        if (ign.contains(" ")) {
            return;
        }

        // checks if ign is even valid
        if (!ign.matches("^[a-zA-Z0-9_]{3,16}$")) {
            event.getMessage().addReaction(Emoji.fromUnicode("❌")).queue();
            return;
        }

        WhitelistData.fetchFromMojang(ign).thenAccept(info -> {
            if (info != null) {
                WhitelistData.register(event.getAuthor().getId(), info.uuid(), info.name());
                if (DiscordIntegrationMod.server != null) {
                    DiscordIntegrationMod.server.execute(() -> {
                        addToWhitelist(info.uuid(), info.name());
                    });
                    event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();
                } else {
                    event.getMessage().addReaction(Emoji.fromUnicode("❓")).queue();
                }
            }
        });
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        WhitelistData.MinecraftInfo info = WhitelistData.get(event.getUser().getId());
        if (info != null && DiscordIntegrationMod.server != null) {
            DiscordIntegrationMod.server.execute(() -> {
                removeFromWhitelist(info.uuid());
                DiscordIntegration.LOGGER.info("User {} left Discord. Removed from whitelist", info.name());
            });
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        WhitelistData.MinecraftInfo info = WhitelistData.get(event.getUser().getId());
        if (info != null && DiscordIntegrationMod.server != null) {
            DiscordIntegrationMod.server.execute(() -> {
                addToWhitelist(info.uuid(), info.name());
                DiscordIntegration.LOGGER.info("User {} joined Discord. Added to whitelist", info.name());
            });
        }
    }

    private void addToWhitelist(String rawUuid, String name) {
        try {
            MinecraftServer server = DiscordIntegrationMod.server;
            UserWhiteList whitelist = server.getPlayerList().getWhiteList();

            UUID uuid = UUID.fromString(formatUuid(rawUuid));
            NameAndId key = new NameAndId(uuid, name);

            if (!whitelist.isWhiteListed(key)) {
                whitelist.add(new UserWhiteListEntry(key));
                whitelist.save();
            }
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Failed to add user to whitelist", e);
        }
    }

    private void removeFromWhitelist(String rawUuid) {
        try {
            MinecraftServer server = DiscordIntegrationMod.server;
            UserWhiteList whitelist = server.getPlayerList().getWhiteList();

            UUID uuid = UUID.fromString(formatUuid(rawUuid));
            NameAndId key = new NameAndId(uuid, null);

            if (whitelist.isWhiteListed(key)) {
                whitelist.remove(key);
                whitelist.save();
            }
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Failed to remove user from whitelist", e);
        }
    }

    private String formatUuid(String id) {
        if (id.contains("-")) return id;
        return id.replaceFirst(
                "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                "$1-$2-$3-$4-$5"
        );
    }
}
