package util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import api.StringList;
import api.StringListImpl;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import core.Context;
import core.EmojiAddingService;
import core.EmojiLoadingService;
import core.SettingsLoader;
import core.TextManipulationService;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class CommandHandler {

    private static final String RAND_FORMAT_ARG = "rf";
    private static final String RAND_EMOJIS_ARG = "re";
    private static final String REPLACE_B_ARG = "rb";

    private AlertService alertService = new AlertService();
    private final Context context;

    public CommandHandler(Context context) {
        this.context = context;
    }

    public void transformText(String command, @Nullable MessageReceivedEvent event) {
        Message message = null;
        Guild guild = null;
        List<Integer> quotations = findQuotations(command);
        String text;
        String args = null;

        if (quotations.isEmpty()) {
            text = command;
        } else if (quotations.size() == 2) {
            args = command.substring(0, quotations.get(0));
            text = command.substring(quotations.get(0) + 1, quotations.get(1));
        } else {
            throw new IllegalArgumentException("Invalid input. See " + DiscordListener.COMMAND_HELP);
        }

        StringBuilder responseBuilder = new StringBuilder();
        boolean randFormat = SettingsLoader.loadBoolProperty("RAND_FORMAT");
        boolean randEmojis = SettingsLoader.loadBoolProperty("RAND_EMOJIS");
        boolean replaceB = SettingsLoader.loadBoolProperty("REPLACE_B");

        if (args != null) {
            StringList argList = StringListImpl.createWords(args).filterWords();
            if (argList.stream().allMatch(arg ->
                arg.equals(RAND_FORMAT_ARG)
                    || arg.equals(RAND_EMOJIS_ARG)
                    || arg.equals(REPLACE_B_ARG))) {
                if (argList.contains(RAND_FORMAT_ARG)) {
                    randFormat = !randFormat;
                }
                if (argList.contains(RAND_EMOJIS_ARG)) {
                    randEmojis = !randEmojis;
                }
                if (argList.contains(REPLACE_B_ARG)) {
                    replaceB = !replaceB;
                }
            } else {
                throw new IllegalArgumentException("Invalid Argument. See " + DiscordListener.COMMAND_HELP);
            }
        }

        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();

        if (event != null) {
            guild = event.getGuild();
            message = event.getMessage();
            List<DiscordEmoji> discordEmojisForGuild = DiscordEmoji.getForGuild(emojiLoadingService.loadDiscordEmojis(), guild.getId());
            emojis.addAll(discordEmojisForGuild);
            responseBuilder.append("**").append(message.getAuthor().getName()).append("**").append(System.lineSeparator());
        }

        TextManipulationService manipulationService = new TextManipulationService(randFormat, randEmojis, replaceB, emojis, guild);
        responseBuilder.append(manipulationService.getOutput(text));
        alertService.send(responseBuilder.toString(), message != null ? message.getChannel() : null);
    }

    /**
     * saves new emojis to xml file
     * <p>
     * Creates new raw emoji if called with following syntax: +e "emoji1, emoji2"
     * Creates new emoji with keywords or adjusts replace value for existing keyword: +e "emoji1, emoji2" "keyword1, keyword2" "true, false"
     *
     * @param command whole command as String
     * @param channel Nullable; use if called from DiscordListener
     */
    public void saveEmojis(String command, @Nullable MessageChannel channel, @Nullable Guild guild) {
        List<Integer> quotations = findQuotations(command);

        //for adding raw emojis with optional random tag (default value: true)
        if (quotations.size() == 2 || quotations.size() == 4) {
            String emojisToAdd = command.substring(quotations.get(0) + 1, quotations.get(1));
            List<String> emojiValues = filterColons(Lists.newArrayList(emojisToAdd.split(", ")));
            StringList randomTags = StringListImpl.create();
            Emoji[] createdEmojis = new Emoji[emojiValues.size()];

            if (quotations.size() == 4) {
                String randomArgsString = command.substring(quotations.get(2) + 1, quotations.get(3));
                List<String> randomArgs = Lists.newArrayList(randomArgsString.split(", "));
                randomTags.addAll(randomArgs);
            }

            if (randomTagsValid(randomTags, emojiValues)) {
                for (int i = 0; i < emojiValues.size(); i++) {
                    String emoji = emojiValues.get(i);
                    boolean random = randomTags.isEmpty() || Boolean.parseBoolean(randomTags.get(i));
                    if (isDiscordEmoji(emoji, guild)) {
                        createdEmojis[i] = new DiscordEmoji(
                            Lists.newArrayList(),
                            getDiscordEmojiValue(emoji, guild),
                            random,
                            emoji,
                            guild.getId(),
                            guild.getName()
                        );
                    } else {
                        createdEmojis[i] = new Emoji(Lists.newArrayList(), emoji, random);
                    }
                }

                context.executePersistTask(true, persistenceManager -> {
                    persistenceManager.addEmojis(createdEmojis);
                    return null;
                }, channel);
            } else {
                alertService.send("Random tags must be either 'true' or 'false'" + System.lineSeparator()
                    + "There has the be one random tag for each emoji, exactly one for all or none at all", channel);
            }
        }

        //for adding emojis with keywords with replace tags for the keywords and optional random tags for the emojis
        else if (quotations.size() == 6 || quotations.size() == 8) {
            int keywordsIndex = quotations.size() == 8 ? 4 : 2;
            int replaceTagsIndex = quotations.size() == 8 ? 6 : 4;

            String emojiString = command.substring(quotations.get(0) + 1, quotations.get(1));
            String keywordString = command.substring(quotations.get(keywordsIndex) + 1, quotations.get(keywordsIndex + 1));
            String replaceTags = command.substring(quotations.get(replaceTagsIndex) + 1, quotations.get(replaceTagsIndex + 1));

            List<String> emojiList = filterColons(Lists.newArrayList(emojiString.split(", ")));
            StringList randomTags = StringListImpl.create();
            String[] keywordList = keywordString.split(", ");
            String[] replaceTagList = replaceTags.split(", ");

            Emoji[] emojis = new Emoji[emojiList.size()];
            Keyword[] keywords = new Keyword[keywordList.length];

            if (quotations.size() == 8) {
                String randomArgsString = command.substring(quotations.get(2) + 1, quotations.get(3));
                List<String> randomArgs = Lists.newArrayList(randomArgsString.split(", "));
                randomTags.addAll(randomArgs);
            }

            for (int i = 0; i < keywordList.length; i++) {
                keywords[i] = new Keyword(keywordList[i], Boolean.parseBoolean(replaceTagList[i]));
            }

            if (keywordsValid(replaceTagList, keywordList) && randomTagsValid(randomTags, emojiList)) {
                for (int i = 0; i < emojiList.size(); i++) {
                    String emoji = emojiList.get(i);
                    boolean random = randomTags.isEmpty() || Boolean.parseBoolean(randomTags.get(i));
                    if (isDiscordEmoji(emoji, guild)) {
                        emojis[i] = new DiscordEmoji(
                            Lists.newArrayList(keywords),
                            getDiscordEmojiValue(emoji, guild),
                            random,
                            emoji,
                            guild.getId(),
                            guild.getName()
                        );
                    } else {
                        emojis[i] = new Emoji(Lists.newArrayList(keywords), emoji, random);
                    }
                }

                context.executePersistTask(true, persistenceManager -> {
                    persistenceManager.addEmojis(emojis);
                    return null;
                }, channel);
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append("Invalid input.");
                if (channel != null) builder.append(" See " + DiscordListener.COMMAND_HELP);
                alertService.send(builder.toString(), channel);
            }
        }

    }

    private boolean keywordsValid(String[] replaceTags, String[] keywords) {
        return Arrays.stream(replaceTags).allMatch(s -> s.equals("true") || s.equals("false"))
            && keywords.length == replaceTags.length
            && Arrays.stream(keywords).allMatch(k -> k.equals(k.toLowerCase()));
    }

    private boolean randomTagsValid(StringList randomTags, List<String> emojis) {
        return randomTags.isEmpty()
            || ((randomTags.size() == 1 || randomTags.size() == emojis.size())
            && randomTags.stream().allMatch(t -> t.equals("false") || t.equals("true")));
    }

    /**
     * deletes emoji when called with following syntax: -e "emoji1, emoji2"
     * deletes keywords from specified emojis: -e "emoji1, emoji2" "keyword1, keyword2"
     *
     * @param command whole command as String
     * @param channel Nullable; use if called from DiscordListener
     */
    public void deleteEmojis(String command, @Nullable MessageChannel channel, @Nullable Guild guild) {
        List<Integer> quotations = findQuotations(command);

        if (quotations.size() == 2) {
            String emojiStrings = command.substring(command.indexOf("\"") + 1, command.lastIndexOf("\""));
            List<String> emojiList = filterColons(Lists.newArrayList(emojiStrings.split(", ")));

            if (guild != null) {
                for (int i = 0; i < emojiList.size(); i++) {
                    String emoji = emojiList.get(i);

                    if (isDiscordEmoji(emoji, guild)) {
                        emojiList.set(i, getDiscordEmojiValue(emoji, guild));
                    }
                }
            }

            context.executePersistTask(true, persistenceManager -> {
                persistenceManager.deleteEmojis(emojiList);
                return null;
            }, channel);
        } else if (quotations.size() == 4) {
            String emojiStrings = command.substring(quotations.get(0) + 1, quotations.get(1));
            String keywords = command.substring(quotations.get(2) + 1, quotations.get(3));

            List<String> emojiList = filterColons(Lists.newArrayList(emojiStrings.split(", ")));
            String[] keywordList = keywords.split(", ");

            if (guild != null) {
                for (int i = 0; i < emojiList.size(); i++) {
                    String emoji = emojiList.get(i);

                    if (isDiscordEmoji(emoji, guild)) {
                        emojiList.set(i, getDiscordEmojiValue(emoji, guild));
                    }
                }
            }

            for (String emoji : emojiList) {
                context.executePersistTask(true, persistenceManager -> {
                    persistenceManager.deleteKeywords(emoji, keywordList);
                    return null;
                }, channel);
            }
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Invalid input.");
            if (channel != null) builder.append(" See " + DiscordListener.COMMAND_HELP);
            alertService.send(builder.toString(), channel);
        }
    }

    /**
     * Search for emojis or keywords.
     * Shows found emoji and lists its keywords or shows found keyword and lists all emojis it occurs on
     *
     * @param searchTerm matches value of keyword or emoji
     * @param channel Nullable; use if called from DiscordListener
     */
    public void searchQuery(String searchTerm, @Nullable MessageChannel channel, @Nullable Guild guild) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        StringBuilder responseBuilder = new StringBuilder();

        if (searchTerm.startsWith(":") && searchTerm.endsWith(":")) {
            searchTerm = searchTerm.substring(1, searchTerm.length() - 1);
        }

        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        List<DiscordEmoji> discordEmojis = emojiLoadingService.loadDiscordEmojis();

        List<Keyword> keywords = Emoji.getAllKeywords(emojis);
        keywords.addAll(Emoji.getAllKeywords(discordEmojis));

        String finalSearchTerm = searchTerm;
        Optional<Emoji> optionalEmoji = emojis.stream().filter(e -> e.getEmojiValue().equals(finalSearchTerm)).findAny();
        Optional<Keyword> optionalKeyword = keywords.stream().filter(k -> k.getKeywordValue().equals(finalSearchTerm)).findAny();
        Optional<DiscordEmoji> optionalDiscordEmoji = discordEmojis.stream()
            .filter(e -> e.getName().equals(finalSearchTerm))
            .findAny();

        if (optionalEmoji.isPresent()) {
            Emoji emoji = optionalEmoji.get();
            responseBuilder.append("\"").append(searchTerm).append("\"").append(" (random: ").append(emoji.isRandom())
                .append(") ").append(" is an emoji.");
            List<Keyword> keywordsOfEmoji = emoji.getKeywords();

            if (!keywordsOfEmoji.isEmpty()) {
                responseBuilder.append(" With following keywords:").append(System.lineSeparator());

                for (Keyword keyword : keywordsOfEmoji) {
                    responseBuilder.append(keyword.getKeywordValue()).append("\t").append(keyword.isReplace())
                        .append(System.lineSeparator());
                }
            }
        }

        if (optionalKeyword.isPresent()) {
            responseBuilder.append("\"").append(searchTerm).append("\"").append(" is a keyword for following emojis:")
                .append(System.lineSeparator());
            List<Emoji> emojisForKeyword = Emoji.loadFromKeyword(optionalKeyword.get(), emojis);
            List<DiscordEmoji> discordEmojisForKeyword = DiscordEmoji.getForKeyword(optionalKeyword.get(), discordEmojis);

            for (Emoji emoji : emojisForKeyword) {
                responseBuilder.append(emoji.getEmojiValue())
                    .append("\treplace: ").append(emoji.requireKeyword(searchTerm).isReplace())
                    .append(System.lineSeparator());
            }

            for (DiscordEmoji discordEmoji : discordEmojisForKeyword) {
                responseBuilder.append(discordEmoji.getEmojiValue())
                    .append("\treplace: ").append(discordEmoji.requireKeyword(searchTerm).isReplace())
                    .append("\tguild: ").append(discordEmoji.getGuildName())
                    .append(System.lineSeparator());
            }
        }

        if (optionalDiscordEmoji.isPresent()) {
            DiscordEmoji discordEmoji = optionalDiscordEmoji.get();
            List<Keyword> keywordsOnEmoji = discordEmoji.getKeywords();

            responseBuilder.append("\"").append(discordEmoji.getEmojiValue()).append("\"").append(" (random: ")
                .append(discordEmoji.isRandom()).append(") ").append(" is an emoji on guild ")
                .append(discordEmoji.getGuildName()).append(System.lineSeparator());

            if (!keywordsOnEmoji.isEmpty()) {
                responseBuilder.append("With following keywords: ").append(System.lineSeparator());

                for (Keyword keyword : keywordsOnEmoji) {
                    responseBuilder.append(keyword.getKeywordValue()).append("\t").append(keyword.isReplace())
                        .append(System.lineSeparator());
                }
            }
        }

        if (!(optionalEmoji.isPresent() || optionalKeyword.isPresent() || optionalDiscordEmoji.isPresent())) {
            responseBuilder.append("No emoji or keyword found for \"").append(searchTerm).append("\"");
        }

        alertService.send(responseBuilder.toString(), channel);
    }

    /**
     * merges duplicate emojis, sets upper case keywords to lower case and merges duplicate keywords on the same emoji
     *
     * @param channel
     */
    public void cleanXml(@Nullable MessageChannel channel) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();

        Set<String> duplicateEmojis = getDuplicateEmojis(emojis);
        List<String> upperCaseKeywords = getUpperCaseKeywords(emojis);
        Multimap<String, String> duplicateKeywords = getDuplicateKeywords(emojis);

        if (duplicateEmojis.isEmpty() && duplicateKeywords.isEmpty() && upperCaseKeywords.isEmpty()) {
            alertService.send("No configuration errors found.", channel);
        } else {
            if (!duplicateEmojis.isEmpty()) {
                emojiAddingService.mergeDuplicateEmojis(duplicateEmojis, channel);
                //check again for duplicate keywords after merging emojis
                emojis = emojiLoadingService.loadEmojis();
                duplicateKeywords = getDuplicateKeywords(emojis);
            }

            if (!upperCaseKeywords.isEmpty()) {
                emojiAddingService.setKeywordsToLowerCase(channel);
                //might also result in duplicate keywords so reload (E, e -> e, e)
                emojis = emojiLoadingService.loadEmojis();
                duplicateKeywords = getDuplicateKeywords(emojis);
            }

            if (!duplicateKeywords.isEmpty()) {
                emojiAddingService.mergeDuplicateKeywords(duplicateKeywords, channel);
            }
        }
    }

    public void handleSettings(String command, @Nullable MessageChannel channel) {
        List<Integer> quotations = findQuotations(command);

        if (quotations.isEmpty() && command.equals(DiscordListener.COMMAND_SETTINGS)) {
            alertService.send(SettingsLoader.displaySettings(), channel);
        } else if (quotations.size() == 2) {
            StringBuilder responseBuilder = new StringBuilder();
            String propertyArgs = command.substring(quotations.get(0) + 1, quotations.get(1));
            String[] properties = propertyArgs.split(", ");

            for (String property : properties) {
                try {
                    responseBuilder.append(SettingsLoader.displaySettings(property)).append(System.lineSeparator());
                } catch (IllegalArgumentException e) {
                    alertService.send(e.getMessage(), channel);
                }
            }

            String response = responseBuilder.toString();
            if (!response.equals("")) alertService.send(response, channel);
        } else if (quotations.size() == 4) {
            String propertyName = command.substring(quotations.get(0) + 1, quotations.get(1));
            String value = command.substring(quotations.get(2) + 1, quotations.get(3));

            if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("true")) {
                try {
                    SettingsLoader.setBoolProperty(propertyName, Boolean.parseBoolean(value));
                    alertService.send(String.format("property %s set to %s", propertyName, value), channel);
                } catch (IllegalArgumentException e) {
                    alertService.send(e.getMessage(), channel);
                }
            } else {
                throw new IllegalArgumentException("Second argument must be boolean");
            }
        } else {
            throw new IllegalArgumentException("Invalid command. See " + DiscordListener.COMMAND_HELP);
        }
    }

    private Set<String> getDuplicateEmojis(List<Emoji> emojis) {
        Set<String> checkedEmojis = Sets.newHashSet();
        Set<String> duplicateEmojis = Sets.newHashSet();

        for (Emoji emoji : emojis) {
            String emojiValue = emoji.getEmojiValue();

            //if emoji value already exists add it to duplicates but only once
            if (!checkedEmojis.contains(emojiValue)) {
                checkedEmojis.add(emojiValue);
            } else {
                duplicateEmojis.add(emojiValue);
            }
        }

        return duplicateEmojis;
    }

    private Multimap<String, String> getDuplicateKeywords(List<Emoji> emojis) {
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

        return emojisWithDuplicateKeywords;
    }

    private List<String> getUpperCaseKeywords(List<Emoji> emojis) {
        List<Keyword> keywords = Emoji.getAllKeywords(emojis);

        return keywords.stream()
            .filter(k -> !k.getKeywordValue().equals(k.getKeywordValue().toLowerCase()))
            .map(Keyword::getKeywordValue)
            .collect(Collectors.toList());
    }

    private List<Integer> findQuotations(String input) {
        List<Integer> positions = Lists.newArrayList();
        for (int i = 0; (i = input.toLowerCase().indexOf("\"", i)) >= 0; i++) {
            positions.add(i);
        }

        return positions;
    }

    private List<String> filterColons(List<String> emojis) {
        for (int i = 0; i < emojis.size(); i++) {
            String emoji = emojis.get(i);
            if (emoji.startsWith(":") && emoji.endsWith(":")) {
                emoji = emoji.substring(1, emoji.length() - 1);
                emojis.set(i, emoji);
            }
        }
        return emojis;
    }

    private String getDiscordEmojiValue(String emojiName, Guild guild) {
        StringBuilder builder = new StringBuilder();
        for (Emote emote : guild.getEmotesByName(emojiName, true)) {
            builder.append(emote.getAsMention());
        }
        return builder.toString();
    }

    private List<String> filterDiscordEmojis(List<String> emojis, Guild guild) {
        return emojis.stream().filter(e -> isDiscordEmoji(e, guild)).collect(Collectors.toList());
    }

    private boolean isDiscordEmoji(String emoji, Guild guild) {
        if (guild != null) {
            return guild.getEmotes().stream().anyMatch(e -> e.getName().equals(emoji));
        } else {
            return false;
        }
    }

}
