package util;

import java.util.List;

import javax.security.auth.login.LoginException;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import core.EmojiLoadingService;
import core.TextLoadingService;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class DiscordListener extends ListenerAdapter {

    public static final String COMMAND_TRANSFORM = "e!e";
    public static final String COMMAND_ADD = "e!add";
    public static final String COMMAND_RM = "e!rm";
    public static final String COMMAND_HELP = "e!help";
    public static final String COMMAND_LIST = "e!list";
    public static final String COMMAND_SEARCH = "e!search";
    public static final String COMMAND_CLEAN = "e!clean";
    public static final String COMMAND_SETTINGS = "e!settings";

    private final CommandHandler commandHandler;

    public DiscordListener(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public void launch() {
        try {
            new JDABuilder(AccountType.BOT)
                .setToken(TextLoadingService.loadToken())
                .addEventListener(this)
                .buildBlocking()
                .getPresence().setGame(Game.playing(COMMAND_HELP));
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentDisplay().startsWith("e!") && !event.getAuthor().isBot()) {
            Message message = event.getMessage();
            String msg = message.getContentDisplay();

            try {
                if (msg.startsWith(COMMAND_TRANSFORM)) {
                    transformText(msg, message, event);
                }

                if (msg.startsWith(COMMAND_ADD)) {
                    commandHandler.saveEmojis(msg, message.getChannel(), event.getGuild());
                }

                if (msg.startsWith(COMMAND_RM)) {
                    commandHandler.deleteEmojis(msg, message.getChannel(), event.getGuild());
                }

                //displays help.txt file
                if (msg.equals(COMMAND_HELP)) {
                    MessageChannel channel = message.getChannel();
                    channel.sendMessage(TextLoadingService.loadHelp()).queue();
                }

                if (msg.equals(COMMAND_LIST)) {
                    listEmojis(message.getChannel());
                }

                if (msg.startsWith(COMMAND_SEARCH)) {
                    searchQuery(message, msg, event.getGuild());
                }

                if (msg.equals(COMMAND_CLEAN)) {
                    commandHandler.cleanXml(message.getChannel());
                }

                if (msg.startsWith(COMMAND_SETTINGS)) {
                    commandHandler.handleSettings(msg, message.getChannel());
                }
            } catch (IllegalArgumentException e) {
                message.getChannel().sendMessage(e.getMessage()).queue();
            }

        }
    }

    /**
     * Replaces spaces with random emojis, replaces B with 🅱 and replaces keywords with emoji or adds emoji after keyword
     * depending on replace is true or false for corresponding keyword
     *
     * @param msg whole input string
     * @param event
     */
    private void transformText(String msg, Message message, MessageReceivedEvent event) {
        try {
            message.delete().queue();
        } finally {
            commandHandler.transformText(msg.substring(COMMAND_TRANSFORM.length() + 1, msg.length()), event);
        }
    }

    /**
     * lists all saved emojis with their keywords and the keywords replace flag
     *
     * @param channel
     */
    private void listEmojis(MessageChannel channel) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        List<DiscordEmoji> discordEmojis = emojiLoadingService.loadDiscordEmojis();
        //if the output exceeds 2000 characters separate into several messages
        List<String> outputParts = Lists.newArrayList();
        outputParts.add("");

        for (Emoji emoji : emojis) {
            StringBuilder builder = new StringBuilder();
            builder.append(emoji.getEmojiValue()).append("\trandom: ").append(emoji.isRandom());

            outputParts = listKeywords(emoji, builder, outputParts);
        }

        if (!discordEmojis.isEmpty()) outputParts.add("Emojis from guilds:\n");
        for (DiscordEmoji discordEmoji : discordEmojis) {
            StringBuilder builder = new StringBuilder();
            builder.append(discordEmoji.getEmojiValue())
                .append("\t").append(discordEmoji.getGuildName())
                .append("\t").append("random: ").append(discordEmoji.isRandom());

            outputParts = listKeywords(discordEmoji, builder, outputParts);
        }

        for (String outputPart : outputParts) {
            channel.sendMessage(outputPart).queue();
        }
    }

    private List<String> listKeywords(Emoji emoji, StringBuilder builder, List<String> outputParts) {
        List<Keyword> keywords = emoji.getKeywords();
        for (int i = 0; i < keywords.size(); i++) {
            if (i == 0) builder.append("\t");

            builder.append(keywords.get(i).getKeywordValue()).append(" (").append(keywords.get(i).isReplace()).append(")");

            if (i < keywords.size() - 1) builder.append(", ");
        }

        builder.append(System.lineSeparator());

        //add to part if character length does not exceed 2000 else create new part
        int lastPart = outputParts.size() - 1;
        if (outputParts.get(lastPart).length() + builder.length() < 2000) {
            outputParts.set(lastPart, outputParts.get(lastPart) + builder.toString());
        } else {
            outputParts.add(builder.toString());
        }

        return outputParts;
    }

    private void searchQuery(Message message, String msg, Guild guild) {
        String query = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));

        commandHandler.searchQuery(query, message.getChannel(), guild);
    }

}
