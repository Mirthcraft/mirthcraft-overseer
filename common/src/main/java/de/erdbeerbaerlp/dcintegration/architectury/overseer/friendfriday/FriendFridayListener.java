package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import de.erdbeerbaerlp.dcintegration.architectury.overseer.OverseerConfig;
import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class FriendFridayListener extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!OverseerConfig.FF_ENABLED) return;
        // check for /friendfriday
        if (event.getName().equals("friendfriday")) {

            // perms check
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ You do not have permission to use this command!").setEphemeral(true).queue();
                return;
            }

            if (FriendFridayManager.isActive()) {
                event.reply("⚠️ Friend Friday is already active!").setEphemeral(true).queue();
                return;
            }

            String inviteCode = event.getOption("invite") != null ? event.getOption("invite").getAsString() : null;

            if (inviteCode == null || inviteCode.isEmpty()) {
                event.reply("❌ You must provide a Discord Invite link!").setEphemeral(true).queue();
                return;
            }

            FriendFridayManager.startEvent(inviteCode);

            event.reply("✅ **Friend Friday started successfully!** The announcement has been posted.").setEphemeral(true).queue();
            return;

        } else if (event.getName().equals("ff-test")) {
            if (!event.getMember().hasPermission(Permission.ADMINISTRATOR)) {
                event.reply("❌ You do not have permission to use this command!").setEphemeral(true).queue();
                return;
            }

            event.reply("done").setEphemeral(true).queue();
            FriendFridayManager.endEvent();

        }
    }

    // only here to give Friend Friday role if not already given by invite
    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        if (!FriendFridayData.isActive()) return;
        var guild = event.getGuild();
        if (OverseerConfig.FF_ROLE_ID.equals("000000000000000000")) return;

        Role ffRole = guild.getRoleById(OverseerConfig.FF_ROLE_ID);
        if (ffRole == null) return;

        if (!event.getMember().getRoles().contains(ffRole)) {
            guild.addRoleToMember(event.getMember(), ffRole).queue(
                    s -> DiscordIntegration.LOGGER.info("Assigned Friend Friday role to {}", event.getUser().getName()),
                    e -> DiscordIntegration.LOGGER.error("Failed to assign Friend Friday role: {}", e.getMessage())
            );
        }
    }

    // there is a chance that with "give role from invite" an active member could get Friend Friday role, remove it
    @Override
    public void onGuildMemberRoleAdd(@NotNull GuildMemberRoleAddEvent event) {
        if (OverseerConfig.FF_ROLE_ID.equals("000000000000000000")) return;
        if (OverseerConfig.ACTIVE_MIRTHER_ROLE_ID.equals("000000000000000000")) return;

        boolean gotFFRole = event.getRoles().stream()
                .anyMatch(role -> role.getId().equals(OverseerConfig.FF_ROLE_ID));

        if (!gotFFRole) return;

        boolean isActiveMirther = event.getMember().getRoles().stream()
                .anyMatch(role -> role.getId().equals(OverseerConfig.ACTIVE_MIRTHER_ROLE_ID));

        if (isActiveMirther) {
            Role ffRole = event.getGuild().getRoleById(OverseerConfig.FF_ROLE_ID);

            if (ffRole != null) {
                event.getGuild().removeRoleFromMember(event.getMember(), ffRole)
                        .reason("User is already an Active Mirther")
                        .queue();
            }
        }
    }
}
