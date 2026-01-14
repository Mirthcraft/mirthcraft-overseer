package de.erdbeerbaerlp.dcintegration.architectury.overseer.friendfriday;

import de.erdbeerbaerlp.dcintegration.architectury.overseer.OverseerConfig;
import net.dv8tion.jda.api.EmbedBuilder;
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
            // if friend friday not active, dont make invite
            if (!FriendFidayManager.isActive()) {
                event.replyEmbeds(new EmbedBuilder()
                        .setColor(Color.RED)
                        .setDescription("It's currently not Friday, please wait till Friday to invite your friend.")
                        .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            event.deferReply(true).queue();

            int duractionSeconds = OverseerConfig.FF_DURATION_HOURS * 3600;
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {

    }
}
