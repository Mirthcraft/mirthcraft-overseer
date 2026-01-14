package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import de.erdbeerbaerlp.dcintegration.architectury.overseer.OverseerConfig;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.*;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FriendFridayManager {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        FriendFridayData.load();
        scheduler.scheduleAtFixedRate(FriendFridayManager::checkTime, 0 ,1, TimeUnit.MINUTES);
    }

    public static boolean isActive() {
        return FriendFridayData.isActive();
    }

    private static void checkTime() {
        if (!OverseerConfig.FF_ENABLED) return;

        if (FriendFridayData.isActive() && System.currentTimeMillis() > FriendFridayData.endTime) {
            endEvent();
        }
    }

    // start friend friday
    public static void startEvent(String inviteLink) {

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
        DayOfWeek endDay = DayOfWeek.valueOf(OverseerConfig.FF_END_DAY.toUpperCase());

        ZonedDateTime targetEnd = now.with(TemporalAdjusters.nextOrSame(endDay))
                .withHour(OverseerConfig.FF_END_HOUR_UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        if (targetEnd.isBefore(now)) {
            targetEnd = targetEnd.plusWeeks(1);
        }

        FriendFridayData.endTime = targetEnd.toInstant().toEpochMilli();
        FriendFridayData.currentInvite = inviteLink;
        FriendFridayData.save();

        DiscordIntegration.LOGGER.info("Friend Friday started! Ends at {}", targetEnd);

        if (!OverseerConfig.FF_ANNOUNCE_CHANNEL_ID.equals("000000000000000000")) {
            TextChannel channel = DiscordIntegration.INSTANCE.getJDA().getTextChannelById(OverseerConfig.FF_ANNOUNCE_CHANNEL_ID);
            if (channel != null) {
                String rolePing = OverseerConfig.FF_PING_ROLE_ID.equals("000000000000000000") ? "" : "<@&" + OverseerConfig.FF_PING_ROLE_ID + "> ";

                long end = FriendFridayData.endTime / 1000;

                channel.sendMessage(rolePing + "\n# **It is Friend Friday!** \n\n" +
                        "For today only (and all following fridays) invite one of your friends to experience Mirthcraft first hand! " +
                        "They will have access to the server until **<t:" + end + ":F>**\n\n" +
                        "**Mirth Members can forward this message or copy this invite:** " + inviteLink + "\n\n" +
                        "-# Note: Friend Friday guests will be removed automatically when friday is over").queue();
            }
        }
    }

    //stop friend friday
    public static void endEvent() {
        DiscordIntegration.LOGGER.info("Friend Friday is over!");
        String inviteLink = FriendFridayData.currentInvite;
        FriendFridayData.clear();

        if (OverseerConfig.GUILD_ID.equals("000000000000000000")) {
            DiscordIntegration.LOGGER.warn("GUILD_ID is not set, cant cleanup Friend Friday.");
            return;
        }

        var guild = DiscordIntegration.INSTANCE.getJDA().getGuildById(OverseerConfig.GUILD_ID);
        if (guild == null) {
            DiscordIntegration.LOGGER.warn("Guild not found, cant cleanup Friend Friday");
            return;
        }

        if (inviteLink != null && !inviteLink.isEmpty()) {
            String code = inviteLink.substring(inviteLink.lastIndexOf('/') + 1);

            guild.retrieveInvites().queue(invites -> {
                for (Invite invite : invites) {
                    if (invite.getCode().equals(code)) {
                        invite.delete().reason("Friend Friday is over!").queue();
                        break;
                    }
                }
            });
        }

        if (OverseerConfig.FF_ROLE_ID.equals("000000000000000000")) {
            DiscordIntegration.LOGGER.warn("FF_ROLE_ID is not set, cant cleanup Friend Friday.");
            return;
        }

        var role = guild.getRoleById(OverseerConfig.FF_ROLE_ID);
        if (role != null) {
            guild.findMembersWithRoles(role).onSuccess(members -> {
                for (Member member : members) {
                    member.getUser().openPrivateChannel().queue(dm -> {
                        dm.sendMessage(OverseerConfig.FF_KICK_DM).queue(
                                s -> guild.kick(member).reason("Friend Friday is over!").queue(),
                                e ->guild.kick(member).reason("Friend Friday is over!").queue()
                        );
                    }, error -> {
                        guild.kick(member).reason("Friend Friday is over!").queue();
                    });
                }
            });
        }
    }
}
