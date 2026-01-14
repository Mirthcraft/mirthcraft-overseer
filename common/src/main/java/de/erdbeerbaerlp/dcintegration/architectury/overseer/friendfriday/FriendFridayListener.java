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

            // will have to send message here

            FriendFridayManager.startEvent("https://www.youtube.com/watch?v=dQw4w9WgXcQ");
            // temp link, will parse from command later
        }
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
        // check if user joined from provided link then give them friend friday role if they dont already have it from the invite (for whatever reason)
    }
}
