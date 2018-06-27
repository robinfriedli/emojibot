package util;

import api.Emoji;
import api.Keyword;
import api.StringList;
import api.StringListImpl;
import com.google.common.collect.Lists;
import core.EmojiLoadingService;
import core.TextLoadingService;
import core.TextManipulationService;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.List;

public class DiscordListener extends ListenerAdapter {

    private static final String COMMAND_TRANSFORM = "e!e";
    private static final String COMMAND_ADD = "e!add";
    private static final String COMMAND_RM = "e!rm";
    private static final String COMMAND_HELP = "e!help";
    private static final String COMMAND_LIST = "e!list";
    private static final String COMMAND_SEARCH = "e!search";
    private static final String COMMAND_CLEAN = "e!clean";

    private final CommandHandler commandHandler;

    public DiscordListener(CommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    public void launch() {
        try {
            new JDABuilder(AccountType.BOT)
                    .setToken(TextLoadingService.loadToken())
                    .addEventListener(this)
                    .buildBlocking();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentDisplay().startsWith("e!") && !event.getAuthor().isBot()) {
            Message message = event.getMessage();
            String msg = message.getContentDisplay();

            if (msg.startsWith(COMMAND_TRANSFORM)) {
                transformText(event, message, msg);
            }

            if (msg.startsWith(COMMAND_ADD)) {
                commandHandler.saveEmojis(msg, message.getChannel());
            }

            if (msg.startsWith(COMMAND_RM)) {
                commandHandler.deleteEmojis(msg, message.getChannel());
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

        }
    }

    /**
     * Replaces spaces with random emojis, replaces B with 🅱 and replaces keywords with emoji or adds emoji after keyword
     * depending on replace is true or false for corresponding keyword
     *
     * @param event
     * @param message
     * @param msg
     */
    private void transformText(MessageReceivedEvent event, Message message, String msg) {
        MessageChannel channel = message.getChannel();

        /*
        Attempted to send profile picture of author as string along with the message.
        Probably hopeless though, String obviously exceeds 2000 characters.
        Also I'm fairly certain it doesn't work that way at all but I'm leaving it in because I wasted a lot of time on this

        String userName = message.getAuthor().getName();
        String avatarString = "";
        try {
            URLConnection connection = new URL(message.getAuthor().getAvatarUrl()).openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.13; rv:60.0) Gecko/20100101 Firefox/60.0");
            connection.connect();
            InputStream inputStream = connection.getInputStream();
            BufferedImage avatar = ImageIO.read(inputStream);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(avatar, "png", bos);
            byte[] imageBytes = bos.toByteArray();

            BASE64Encoder encoder = new BASE64Encoder();
            avatarString = encoder.encode(imageBytes);

            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

        try {
            message.delete().queue();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }

        String input = msg.substring(COMMAND_TRANSFORM.length() + 1, msg.length());
        StringBuilder response = new StringBuilder();
        boolean randFormat = false;

        if (input.startsWith("-r ")) {
            randFormat = true;
            input = input.substring(3, input.length());
        }

        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        TextManipulationService service = new TextManipulationService(randFormat, emojiLoadingService.loadEmojis());

        response.append("**").append(message.getAuthor().getName()).append(":").append("**").append(System.lineSeparator())
                .append(service.getOutput(input));

        String output = response.toString();
        if (output.length() < 2000) {
            channel.sendMessage(output).queue();
        } else {
            List<String> outputParts = separateMessage(output);
            for (String part : outputParts) {
                channel.sendMessage(part).queue();
            }
        }
    }

    private List<String> separateMessage(String message) {
        List<String> outputParts = Lists.newArrayList();
        //first split the message into paragraphs (Lists.newArrayList() because Arrays.asList returns immutable list)
        List<String> paragraphs = Lists.newArrayList(Arrays.asList(message.split("\n")));

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            if (paragraph.length() < 2000) {
                outputParts.add(paragraph);
                if (i < paragraphs.size() - 1) outputParts.add(System.lineSeparator());
            } else {
                //if the paragraph is too long separate into sentences
                StringList sentences = StringListImpl.createSentences(paragraph);
                for (String sentence : sentences) {
                    //check if sentence is shorter than 2000 characters, else split into words
                    if (sentence.length() < 2000) {
                        //since we don't want to send a message for each sentence fill the same part until full
                        outputParts = fillParts(outputParts, sentence);
                    } else {
                        //if sentence is longer than 2000 characters split into words
                        StringList words = StringListImpl.createWords(sentence);

                        for (String word : words) {
                            if (word.length() < 2000) {
                                outputParts = fillParts(outputParts, word);
                            } else {
                                //this should never happen since discord does not allow you to send messages longer than
                                // 2000 characters and if there are no spaces there are no emojis to make the text longer
                                StringList chars = StringListImpl.charsToList(word);
                                for (String charString : chars) {
                                    outputParts = fillParts(outputParts, charString);
                                }
                            }
                        }
                    }
                }
            }
        }

        return outputParts;
    }

    private List<String> fillParts(List<String> outputParts, String s) {
        if (outputParts.isEmpty()) outputParts.add("");
        int currentPart = outputParts.size() - 1;

        if (outputParts.get(currentPart).length() + s.length() < 2000) {
            outputParts.set(currentPart, outputParts.get(currentPart) + s);
        } else {
            outputParts.add(s);
        }

        return outputParts;
    }

    /**
     * lists all saved emojis with their keywords and the keywords replace flag
     *
     * @param channel
     */
    private void listEmojis(MessageChannel channel) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        //if the output exceeds 2000 characters separate into several messages
        List<String> outputParts = Lists.newArrayList();
        outputParts.add("");

        for (Emoji emoji : emojis) {
            StringBuilder builder = new StringBuilder();
            builder.append(emoji.getEmojiValue());

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

        for (String outputPart : outputParts) {
            channel.sendMessage(outputPart).queue();
        }
    }

    private void searchQuery(Message message, String msg) {
        String query = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));

        commandHandler.searchQuery(query, message.getChannel());
    }

}
