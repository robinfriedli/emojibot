package util;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import core.PersistenceManager;
import core.TextLoadingService;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.ContextManager;

import javax.security.auth.login.LoginException;
import java.util.List;

public class DiscordListener extends ListenerAdapter {

    public static final String COMMAND_TRANSFORM = "e!e";
    public static final String COMMAND_WHISPER = "e!whisper";
    public static final String COMMAND_ADD = "e!add";
    public static final String COMMAND_RM = "e!rm";
    public static final String COMMAND_HELP = "e!help";
    public static final String COMMAND_LIST = "e!list";
    public static final String COMMAND_SEARCH = "e!search";
    public static final String COMMAND_CLEAN = "e!clean";
    public static final String COMMAND_SETTINGS = "e!settings";

    private final ContextManager contextManager;
    private final CommandHandler commandHandler;
    private Mode mode;
    private Guild guild;

    public DiscordListener(ContextManager contextManager, CommandHandler commandHandler) {
        this.contextManager = contextManager;
        this.commandHandler = commandHandler;
    }

    public void launch(Mode mode) {
        try {
            JDA jda = new JDABuilder(AccountType.BOT)
                .setToken(TextLoadingService.loadToken())
                .addEventListener(this)
                .buildBlocking();
            jda.getPresence().setGame(Game.playing(COMMAND_HELP));

            if (mode == Mode.PARTITIONED) {
                List<Guild> guilds = jda.getGuilds();
                for (Guild guild : guilds) {
                    contextManager.createBoundContext(guild, guild.getId(), new PersistenceManager());
                }
            }
            setMode(mode);
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildJoin(GuildJoinEvent event) {
        if (getMode() == Mode.PARTITIONED) {
            contextManager.createBoundContext(event.getGuild(), event.getGuild().getId(), new PersistenceManager());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentDisplay().startsWith("e!") && !event.getAuthor().isBot()) {
            if (mode == Mode.PARTITIONED) {
                guild = event.getGuild();
                commandHandler.setContext(contextManager.getContext(guild));
            }
            Message message = event.getMessage();
            String msg = message.getContentDisplay();

            try {
                if (msg.startsWith(COMMAND_TRANSFORM)) {
                    transformText(msg, message, event);
                }

                if (msg.startsWith(COMMAND_WHISPER)) {
                    whisper(msg, message, event);
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
                    searchQuery(message, msg);
                }

                if (msg.equals(COMMAND_CLEAN)) {
                    commandHandler.cleanXml(message.getChannel());
                }

                if (msg.startsWith(COMMAND_SETTINGS)) {
                    commandHandler.handleSettings(msg, message.getChannel());
                }

                if (msg.equals("e!test")) {
                    Context context = contextManager.getContext(guild);
                    context.invoke(false, () -> {
                        new Emoji(Lists.newArrayList(), "mine",true ,context);

                        Emoji craft = context.invoke(false, () -> new Emoji(Lists.newArrayList(), "craft", true, context));
                        System.out.println(craft.toString());

                    });
                    context.commitAll();
                }
            } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException | AssertionError e) {
                e.printStackTrace();
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
            commandHandler.transformText(msg.substring(COMMAND_TRANSFORM.length() + 1), event, false);
        }
    }

    private void whisper(String msg, Message message, MessageReceivedEvent event) {
        try {
            message.delete().queue();
        } finally {
            commandHandler.transformText(msg.substring(COMMAND_WHISPER.length() + 1), event, true);
        }
    }

    /**
     * lists all saved emojis with their keywords and the keywords replace flag
     *
     * @param channel channel to send message to
     */
    @SuppressWarnings("unchecked") // cast is safe since if the Element is not a DiscordEmoji it must be an Emoji
    private void listEmojis(MessageChannel channel) {
        Context context;
        if (getMode() == Mode.PARTITIONED) {
            context = contextManager.getContext(guild);
        } else {
            context = contextManager.getContext();
        }

        List<Emoji> emojis = (List<Emoji>) context.getInstancesOf(Emoji.class, DiscordEmoji.class);
        List<DiscordEmoji> discordEmojis = context.getInstancesOf(DiscordEmoji.class);
        //if the output exceeds 2000 characters separate into several messages
        List<String> outputParts = Lists.newArrayList();
        outputParts.add("");

        for (Emoji emoji : emojis) {
            StringBuilder builder = new StringBuilder();
            builder.append(emoji.getEmojiValue()).append("\trandom: ").append(emoji.isRandom());

            listKeywords(emoji, builder, outputParts);
        }

        if (!discordEmojis.isEmpty()) outputParts.add("Emojis from guilds:\n");
        for (DiscordEmoji discordEmoji : discordEmojis) {
            StringBuilder builder = new StringBuilder();
            builder.append(discordEmoji.getEmojiValue())
                .append("\t").append(discordEmoji.getGuildName())
                .append("\t").append("random: ").append(discordEmoji.isRandom());

            listKeywords(discordEmoji, builder, outputParts);
        }

        for (String outputPart : outputParts) {
            channel.sendMessage(outputPart).queue();
        }
    }

    private void listKeywords(Emoji emoji, StringBuilder builder, List<String> outputParts) {
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
    }

    private void searchQuery(Message message, String msg) {
        String query = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));

        commandHandler.searchQuery(query, message.getChannel());
    }

    private void setMode(Mode mode) {
        this.mode = mode;
    }

    private Mode getMode() {
        return mode;
    }

    public enum Mode {
        /**
         * all guilds share the same {@link Context} meaning all emojis will be available across all guilds
         *
         * Recommended if you want to be able to use guild emotes on other guilds
         */
        SHARED,

        /**
         * there will be a separate {@link Context.BindableContext} for each guild meaning the emojis will be separated
         *
         * Recommended if one emojibot instance is shared by completely different guilds
         */
        PARTITIONED
    }

}
