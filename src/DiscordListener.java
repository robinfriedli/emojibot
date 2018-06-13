import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DiscordListener extends ListenerAdapter {

    private static final String COMMAND_TRANSFORM = "e!e";
    private static final String COMMAND_ADD = "e!add";
    private static final String COMMAND_RM = "e!rm";
    private static final String COMMAND_HELP = "e!help";
    private static final String COMMAND_LIST = "e!list";
    private static final String COMMAND_SEARCH = "e!search";

    public static void main(String[] args) {
        try {
            new JDABuilder(AccountType.BOT)
                    .setToken(TextLoadingService.loadToken())
                    .addEventListener(new DiscordListener())
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
                saveEmojis(message, msg);
            }

            if (msg.startsWith(COMMAND_RM)) {
                deleteEmojis(message, msg);
            }

            //displays help.txt file
            if (msg.equals(COMMAND_HELP)) {
                MessageChannel channel = message.getChannel();
                channel.sendMessage(TextLoadingService.loadHelp()).queue();
            }

            if (msg.equals(COMMAND_LIST)) {
                listEmojis(message);
            }

            if (msg.startsWith(COMMAND_SEARCH)) {
                searchQuery(message, msg);
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

        if (event.isFromType(ChannelType.TEXT)) {
            message.delete().queue();
        }

        String input = msg.substring(COMMAND_TRANSFORM.length() + 1, msg.length());
        StringBuilder response = new StringBuilder();

        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        TextManipulationService service = new TextManipulationService(false, emojiLoadingService.loadEmojis());

        response.append("**").append(message.getAuthor().getName()).append(":").append("**").append(System.lineSeparator())
                .append(service.getOutput(input));
        channel.sendMessage(response.toString()).queue();
    }

    /**
     * saves new emojis to xml file
     * <p>
     * Creates new raw emoji if called with following syntax: +e "emoji1, emoji2"
     * Creates new emoji with keywords or adjusts replace value for existing keyword: +e "emoji1, emoji2" "keyword1, keyword2" "true, false"
     *
     * @param message
     * @param msg
     */
    private void saveEmojis(Message message, String msg) {
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        MessageChannel channel = message.getChannel();
        List<Integer> quotations = findOccurrences(msg, "\"");

        //if following syntax is used: +e "emoji1, emoji2"
        if (quotations.size() == 2) {
            String emojis = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));
            String[] emojiList = emojis.split(", ");
            emojiAddingService.addEmojis(emojiList);

        }

        //if following syntax is used: +e "emoji1, emoji2" "keyword1, keyword2" "true, false"
        else if (quotations.size() == 6) {
            String emojis = msg.substring(quotations.get(0) + 1, quotations.get(1));
            String keywords = msg.substring(quotations.get(2) + 1, quotations.get(3));
            String replaceTags = msg.substring(quotations.get(4) + 1, quotations.get(5));

            String[] emojiList = emojis.split(", ");
            String[] keywordList = keywords.split(", ");
            String[] replaceTagList = replaceTags.split(", ");

            if (Arrays.stream(replaceTagList).allMatch(s -> s.equals("true") || s.equals("false"))
                    && keywordList.length == replaceTagList.length) {
                emojiAddingService.addEmojis(emojiList, keywordList, replaceTagList);
            } else {
                channel.sendMessage("There has to be one replace flag for each keyword. " +
                        "Replace tag has to be either 'true' or 'false'" +
                        "\nSee 'e!help'").queue();
            }
        } else {
            channel.sendMessage("Invalid input. See 'e!help'").queue();
        }
    }

    /**
     * deletes emoji when called with following syntax: -e "emoji1, emoji2"
     * deletes keywords from specified emojis: -e "emoji1, emoji2" "keyword1, keyword2"
     *
     * @param message
     * @param msg
     */
    private void deleteEmojis(Message message, String msg) {
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        MessageChannel channel = message.getChannel();
        List<Integer> quotations = findOccurrences(msg, "\"");

        if (quotations.size() == 2) {
            String emojis = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));
            String[] emojiList = emojis.split(", ");
            emojiAddingService.removeEmojis(Arrays.asList(emojiList));
        } else if (quotations.size() == 4) {
            String emojis = msg.substring(quotations.get(0) + 1, quotations.get(1));
            String keywords = msg.substring(quotations.get(2) + 1, quotations.get(3));

            String[] emojiList = emojis.split(", ");
            String[] keywordList = keywords.split(", ");

            emojiAddingService.removeKeywords(Arrays.asList(emojiList), Arrays.asList(keywordList));
        } else {
            channel.sendMessage("Invalid input. See 'e!help'").queue();
        }
    }

    /**
     * lists all saved emojis with their keywords and the keywords replace flag
     *
     * @param message
     */
    private void listEmojis(Message message) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        StringBuilder builder = new StringBuilder();

        for (Emoji emoji : emojis) {
            builder.append(emoji.getEmojiValue()).append(System.lineSeparator());

            for (Keyword keyword : emoji.getKeywords()) {
                builder.append("\t").append(keyword.getKeywordValue()).append("\t").append(keyword.isReplace())
                        .append(System.lineSeparator());
            }

            builder.append(System.lineSeparator());
        }

        MessageChannel channel = message.getChannel();
        channel.sendMessage(builder.toString()).queue();
    }

    /**
     * Search for emojis or keywords.
     * Shows found emoji and lists its keywords or shows found keyword and lists all emojis it occurs on
     *
     * @param message
     * @param msg
     */
    private void searchQuery(Message message, String msg) {
        String query = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        StringBuilder responseBuilder = new StringBuilder();

        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        List<Keyword> keywords = Emoji.getAllKeywords(emojis);

        Optional<Emoji> optionalEmoji = emojis.stream().filter(e -> e.getEmojiValue().equals(query)).findAny();
        Optional<Keyword> optionalKeyword = keywords.stream().filter(k -> k.getKeywordValue().equals(query)).findAny();

        if (optionalEmoji.isPresent()) {
            responseBuilder.append("\"").append(query).append("\"").append(" is an emoji.");
            List<Keyword> keywordsOfEmoji = optionalEmoji.get().getKeywords();

            if (!keywordsOfEmoji.isEmpty()) {
                responseBuilder.append(" With following keywords:").append(System.lineSeparator());

                for (Keyword keyword : keywordsOfEmoji) {
                    responseBuilder.append(keyword.getKeywordValue()).append("\t").append(keyword.isReplace())
                            .append(System.lineSeparator());
                }
            }
        }

        if (optionalKeyword.isPresent()) {
            responseBuilder.append("\"").append(query).append("\"").append(" is a keyword for following emojis:")
                    .append(System.lineSeparator());
            List<Emoji> emojisForKeyword = Emoji.loadFromKeyword(optionalKeyword.get(), emojis);

            for (Emoji emoji : emojisForKeyword) {
                responseBuilder.append(emoji.getEmojiValue());
            }
        }

        if (!(optionalEmoji.isPresent() || optionalKeyword.isPresent())) {
            responseBuilder.append("No emoji or keyword found for \"").append(query).append("\"");
        }

        MessageChannel channel = message.getChannel();
        channel.sendMessage(responseBuilder.toString()).queue();
    }

    private List<Integer> findOccurrences(String input, String keyword) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; (i = input.toLowerCase().indexOf(keyword, i)) >= 0; i++) {
            positions.add(i);
        }

        return positions;
    }

}
