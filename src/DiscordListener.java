import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;
import java.util.*;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter {

    private static final String COMMAND_TRANSFORM = "e!e";
    private static final String COMMAND_ADD = "e!add";
    private static final String COMMAND_RM = "e!rm";
    private static final String COMMAND_HELP = "e!help";
    private static final String COMMAND_LIST = "e!list";
    private static final String COMMAND_SEARCH = "e!search";
    private static final String COMMAND_CLEAN = "e!clean";

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

            if (msg.equals(COMMAND_CLEAN)) {
                cleanDuplicateEmojis();
                cleanDuplicateKeywords();
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

        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        TextManipulationService service = new TextManipulationService(false, emojiLoadingService.loadEmojis());

        response.append("**").append(message.getAuthor().getName()).append(":").append("**").append(System.lineSeparator())
                .append(service.getOutput(input));

        try {
            channel.sendMessage(response.toString()).queue();
        } catch (IllegalArgumentException e) {
            channel.sendMessage(e.getMessage()).queue();
        }
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
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        AlertService alertService = new AlertService();

        MessageChannel channel = message.getChannel();
        List<Emoji> allEmojis = emojiLoadingService.loadEmojis();
        List<Integer> quotations = findOccurrences(msg, "\"");

        //if following syntax is used: +e "emoji1, emoji2"
        if (quotations.size() == 2) {
            String emojis = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));
            String[] emojiList = emojis.split(", ");

            emojiAddingService.addEmojis(emojiList);
            alertService.alertAddedEmojis(emojiList, allEmojis, channel);
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
                    && keywordList.length == replaceTagList.length
                    && Arrays.stream(keywordList).allMatch(k -> k.equals(k.toLowerCase()))) {
                try {
                    emojiAddingService.addEmojis(emojiList, keywordList, replaceTagList);
                    alertService.alertAddedEmojis(emojiList, keywordList, replaceTagList, allEmojis, channel);
                } catch (IllegalStateException e) {
                    channel.sendMessage(e.getMessage()).queue();
                }
            } else {
                channel.sendMessage("There has to be one replace flag for each keyword" +
                        "\nReplace tag has to be either 'true' or 'false'" +
                        "\nKeywords have to be lower case" +
                        "\nSee '" + COMMAND_HELP + "'").queue();
            }
        } else {
            channel.sendMessage("Invalid input. See '" + COMMAND_HELP + "'").queue();
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
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        AlertService alertService = new AlertService();

        MessageChannel channel = message.getChannel();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        List<Integer> quotations = findOccurrences(msg, "\"");

        if (quotations.size() == 2) {
            String emojiStrings = msg.substring(msg.indexOf("\"") + 1, msg.lastIndexOf("\""));
            List<String> emojiList = Lists.newArrayList(Arrays.asList(emojiStrings.split(", ")));
            List<String> missingEmojis = emojiList.stream().filter(e -> !alertService.emojiExists(e, emojis))
                    .collect(Collectors.toList());

            if (!missingEmojis.isEmpty()) {
                alertService.alertMissingEmojis(missingEmojis, channel);
                emojiList.removeAll(missingEmojis);
            }

            if (!emojiList.isEmpty()) {
                emojiAddingService.removeEmojis(emojiList);
                alertService.alertRemovedEmojis(emojiList, channel);
            }
        } else if (quotations.size() == 4) {
            String emojiStrings = msg.substring(quotations.get(0) + 1, quotations.get(1));
            String keywords = msg.substring(quotations.get(2) + 1, quotations.get(3));

            List<String> emojiList = Lists.newArrayList(Arrays.asList(emojiStrings.split(", ")));
            List<String> keywordList = Lists.newArrayList(Arrays.asList(keywords.split(", ")));
            List<String> missingEmojis = emojiList.stream().filter(e -> !alertService.emojiExists(e, emojis))
                    .collect(Collectors.toList());

            if (!missingEmojis.isEmpty()) {
                alertService.alertMissingEmojis(missingEmojis, channel);
                emojiList.removeAll(missingEmojis);
            }

            for (String keyword : keywordList) {
                if (emojiList.stream().anyMatch(e -> !alertService.keywordExists(keyword, e, emojis))) {
                    alertService.alertMissingKeyword(keyword, emojiList, emojis, channel);
                }
            }

            if (!emojiList.isEmpty()) {
                emojiAddingService.removeKeywords(emojiList, keywordList);
                alertService.alertRemovedKeywords(keywordList, emojiList, emojis, channel);
            }
        } else {
            channel.sendMessage("Invalid input. See '" + COMMAND_HELP + "'").queue();
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

        MessageChannel channel = message.getChannel();
        for (String outputPart : outputParts) {
            channel.sendMessage(outputPart).queue();
        }
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

    private void cleanDuplicateEmojis() {
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();

        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        List<String> checkedEmojis = Lists.newArrayList();
        List<String> duplicateEmojis = Lists.newArrayList();

        for (Emoji emoji : emojis) {
            String emojiValue = emoji.getEmojiValue();

            //if emoji value already exists add it to duplicates but only once
            if (!checkedEmojis.contains(emojiValue)) {
                checkedEmojis.add(emojiValue);
            } else if (!duplicateEmojis.contains(emojiValue)) {
                duplicateEmojis.add(emojiValue);
            }
        }

        emojiAddingService.mergeDuplicateEmojis(duplicateEmojis);
    }

    private void cleanDuplicateKeywords() {
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();

        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        Multimap<String, String> emojisWithDuplicateKeywords = ArrayListMultimap.create();

        for (Emoji emoji : emojis) {
            List<Keyword> keywords = emoji.getKeywords();
            List<String> checkedKeywords = Lists.newArrayList();

            for (Keyword keyword : keywords) {
                String keywordValue = keyword.getKeywordValue();
                //check if keyword has come up already
                if (!checkedKeywords.contains(keywordValue)) {
                    checkedKeywords.add(keywordValue);
                }
                //if keyword comes up again, meaning it is duplicate, check if emoji is already in the multimap
                else if (!emojisWithDuplicateKeywords.containsKey(emoji.getEmojiValue())) {
                    emojisWithDuplicateKeywords.put(emoji.getEmojiValue(), keywordValue);
                }
                //if emoji was already added check if the same particular keyword has already been added
                else if (!emojisWithDuplicateKeywords.get(emoji.getEmojiValue()).contains(keywordValue)) {
                    emojisWithDuplicateKeywords.put(emoji.getEmojiValue(), keywordValue);
                }
            }
        }

        emojiAddingService.mergeDuplicateKeywords(emojisWithDuplicateKeywords);
    }

    private List<Integer> findOccurrences(String input, String keyword) {
        List<Integer> positions = Lists.newArrayList();
        for (int i = 0; (i = input.toLowerCase().indexOf(keyword, i)) >= 0; i++) {
            positions.add(i);
        }

        return positions;
    }

}
