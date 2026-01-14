package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import de.erdbeerbaerlp.dcintegration.architectury.overseer.OverseerConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
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

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        // check if user joined from provided link then give them friend friday role if they dont already have it from the invite (for whatever reason)
    }
}
