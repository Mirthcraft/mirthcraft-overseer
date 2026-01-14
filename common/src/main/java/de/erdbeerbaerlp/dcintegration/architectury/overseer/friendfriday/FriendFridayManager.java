package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import de.erdbeerbaerlp.dcintegration.architectury.overseer.OverseerConfig;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FriendFridayManager {
    private static boolean isActive = false;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void init() {
        scheduler.scheduleAtFixedRate(FriendFridayManager::checkTime, 0 ,1, TimeUnit.MINUTES);
    }

    public static boolean isActive() {
        return isActive;
    }

    private static void checkTime() {
        if (!OverseerConfig.FF_ENABLED) return;

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        ZonedDateTime weekStart = now.with(DayOfWeek.FRIDAY)
                .withHour(OverseerConfig.FF_START_HOUR_UTC)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);

        ZonedDateTime weekEnd = weekStart.plusHours(OverseerConfig.FF_DURATION_HOURS);

        if (now.getDayOfWeek() != DayOfWeek.FRIDAY && now.isBefore(weekStart)) {
            weekStart = weekStart.minusWeeks(1);
            weekEnd = weekStart.plusHours(OverseerConfig.FF_DURATION_HOURS);
        }

        boolean insideWindow = now.isAfter(weekStart) && now.isBefore(weekEnd);

        if (insideWindow && !isActive) {
            startEvent();
        } else if (!insideWindow && isActive) {
            endEvent();
        }
    }

    // start friend friday
    private static void startEvent() {
        isActive = true;
        DiscordIntegration.LOGGER.info("Friend Friday is active!");

        if (!OverseerConfig.FF_ANNOUNCE_CHANNEL_ID.equals("000000000000000000")) {
            TextChannel channel = DiscordIntegration.INSTANCE.getJDA().getTextChannelById(OverseerConfig.FF_ANNOUNCE_CHANNEL_ID);
            if (channel != null) {
                String rolePing = OverseerConfig.FF_PING_ROLE_ID.equals("000000000000000000") ? "" : "<@&" + OverseerConfig.FF_PING_ROLE_ID + "> ";

                channel.sendMessage(rolePing + "\n# **It is Friend Friday!** \n\n" +
                        "For today only (and all following fridays) invite your friends to experience Mirthcraft first hand! " +
                        " They will have access to the server until the end of Friday (maybe a little bit over).\n\n" +
                        "Mirth Members can can use `/invite` to generate a server invite to send\n\n" +
                        "-# Note: Friend Friday guests will be removed automatically when friday is over").queue();
            }
        }
    }

    //stop friend friday
    private static void endEvent() {
        isActive = false;
        DiscordIntegration.LOGGER.info("Friend Friday is over!");

        if (!OverseerConfig.FF_ANNOUNCE_CHANNEL_ID.equals("000000000000000000")) return;

        var channel = DiscordIntegration.INSTANCE.getJDA().getTextChannelById(OverseerConfig.FF_ANNOUNCE_CHANNEL_ID);
        if (channel == null) return;
        var guild = channel.getGuild();

        var role = guild.getRoleById(OverseerConfig.FF_ROLE_ID);
        if (role == null) return;

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
