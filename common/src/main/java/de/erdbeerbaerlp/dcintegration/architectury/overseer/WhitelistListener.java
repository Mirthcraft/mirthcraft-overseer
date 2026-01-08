package de.erdbeerbaerlp.dcintegration.architectury.overseer;

import de.erdbeerbaerlp.dcintegration.common.DiscordIntegration;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

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

        if (DiscordIntegration.INSTANCE != null) {
            System.out.println("attempting to whitelist: " + ign);
            DiscordIntegration.INSTANCE.getServerInterface().runMcCommand("whitelist add " + ign, null, event.getAuthor());
            event.getMessage().addReaction(Emoji.fromUnicode("✅")).queue();
        }
    }
}
