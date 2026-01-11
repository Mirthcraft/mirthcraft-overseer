package de.erdbeerbaerlp.dcintegration.architectury.overseer;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import de.erdbeerbaerlp.dcintegration.architectury.DiscordIntegrationMod;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.server.players.NameAndId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class WhitelistListener extends ListenerAdapter {

    private boolean isValid(String value) {
        return value != null && !value.isBlank() && !value.equals("000000000000000000");
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!OverseerConfig.ENABLED) return;
        if (!isValid(OverseerConfig.WHITELIST_CHANNEL_ID)) return;
        if (!event.getChannel().getId().equals(OverseerConfig.WHITELIST_CHANNEL_ID)) return;
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
                WhitelistData.LinkResult result = WhitelistData.register(event.getAuthor().getId(), info.uuid(), info.name());

                if (result == WhitelistData.LinkResult.SUCCESS) {
                    if (DiscordIntegrationMod.server != null) {
                        DiscordIntegrationMod.server.execute(() -> {
                            addToWhitelist(info.uuid(), info.name());
                        });
                        event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();
                    }

                } else if (result == WhitelistData.LinkResult.ALREADY_OWNED) {
                    event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();

                } else if (result == WhitelistData.LinkResult.TAKEN_BY_OTHER) {
                    event.getMessage().addReaction(Emoji.fromUnicode("⛔")).queue();

                }
            } else {
                event.getMessage().addReaction(Emoji.fromUnicode("❓")).queue();
            }
        });
    }

    @Override
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        List<WhitelistData.MinecraftInfo> accounts = WhitelistData.get(event.getUser().getId());
        if (accounts != null && !accounts.isEmpty() && DiscordIntegrationMod.server != null) {
            DiscordIntegrationMod.server.execute(() -> {
                for (WhitelistData.MinecraftInfo info : accounts) {
                    removeFromWhitelist(info.uuid());
                    DiscordIntegration.LOGGER.info("User {} left Discord. Removed {} from whitelist", event.getUser().getName(), info.name());
                }
            });
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        List<WhitelistData.MinecraftInfo> accounts = WhitelistData.get(event.getUser().getId());
        if (accounts != null && !accounts.isEmpty() && DiscordIntegrationMod.server != null) {
            DiscordIntegrationMod.server.execute(() -> {
                for (WhitelistData.MinecraftInfo info : accounts) {
                    addToWhitelist(info.uuid(), info.name());
                    DiscordIntegration.LOGGER.info("User {} joined Discord. Added {} to whitelist", event.getUser().getName(), info.name());
                }
            });
        }
    }

    // run at start up in case someone leaves while server down
    public void runStartupSync() {
        if (!OverseerConfig.ENABLED) return;
        if (!OverseerConfig.SYNC_ON_STARTUP) return;
        if (!isValid(OverseerConfig.WHITELIST_CHANNEL_ID)) return;

        var channel = DiscordIntegration.INSTANCE.getChannel(OverseerConfig.WHITELIST_CHANNEL_ID);
        if (channel == null) return;
        var guild = channel.getGuild();

        DiscordIntegration.LOGGER.info("[The Overseer] Running Startup Sync");

        Set<String> allDiscordIds = WhitelistData.getAllDiscordIds();

        for (String discordId : allDiscordIds) {
            guild.retrieveMemberById(discordId).queue(
                    success -> {
                        // do nothing for now but later check if changed to "former mirther"

                        List<WhitelistData.MinecraftInfo> accounts = WhitelistData.get(discordId);
                        if (accounts != null && DiscordIntegrationMod.server != null) {
                            DiscordIntegrationMod.server.execute(() -> {
                                for (WhitelistData.MinecraftInfo info : accounts) {
                                    if (addToWhitelist(info.uuid(), info.name())) {
                                        DiscordIntegration.LOGGER.info("[Sync] User {} joined the Discord. Added to whitelist.", info.name());
                                    }
                                }
                            });
                        }
                    },
                    error -> {
                        // user not found so likely left
                        if (error instanceof ErrorResponseException e) {
                            if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MEMBER) {
                                List<WhitelistData.MinecraftInfo> accounts = WhitelistData.get(discordId);
                                if (accounts != null && DiscordIntegrationMod.server != null) {
                                    DiscordIntegrationMod.server.execute(() -> {
                                        for (WhitelistData.MinecraftInfo info : accounts) {
                                            removeFromWhitelist(info.uuid());
                                            DiscordIntegration.LOGGER.info("[Sync] User {} is no longer in Discord. Removed from whitelist.", info.name());
                                        }
                                    });
                                }
                            }
                        }
                    }
            );
        }
    }

    private boolean addToWhitelist(String rawUuid, String name) {
        try {
            MinecraftServer server = DiscordIntegrationMod.server;
            UserWhiteList whitelist = server.getPlayerList().getWhiteList();

            UUID uuid = UUID.fromString(formatUuid(rawUuid));
            NameAndId key = new NameAndId(uuid, name);

            if (!whitelist.isWhiteListed(key)) {
                whitelist.add(new UserWhiteListEntry(key));
                whitelist.save();
                return true;
            }
        } catch (Exception e) {
            DiscordIntegration.LOGGER.error("Failed to add user to whitelist", e);
        }
        return false;
    }

    private void removeFromWhitelist(String rawUuid) {
        try {
            MinecraftServer server = DiscordIntegrationMod.server;
            UserWhiteList whitelist = server.getPlayerList().getWhiteList();

            UUID uuid = UUID.fromString(formatUuid(rawUuid));
            // keep note of this because it could stop working in future update (passing in null here)
            NameAndId key = new NameAndId(uuid, null);

            if (whitelist.isWhiteListed(key)) {
                whitelist.remove(key);
                whitelist.save();
            }

            if (OverseerConfig.FORCE_KICK) {
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    player.connection.disconnect(Component.literal("You have been removed from the whitelist"));
                }
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
