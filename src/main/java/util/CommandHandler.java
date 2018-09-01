package util;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.*;
import core.PersistenceManager;
import core.SettingsLoader;
import core.TextManipulationService;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.stringlist.StringList;
import net.robinfriedli.stringlist.StringListImpl;

import javax.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class CommandHandler {

    private static final Map<String, String> ARG_MAP;

    static {
        ARG_MAP = new HashMap<>();
        ARG_MAP.put("RAND_FORMAT_ARG", "rf");
        ARG_MAP.put("RAND_EMOJIS_ARG", "re");
        ARG_MAP.put("REPLACE_B_ARG", "rb");
        ARG_MAP.put("REPLACE_WORDPART_ARG", "rw");
    }

    private AlertService alertService = new AlertService();
    private Context context;

    public CommandHandler(Context context) {
        this.context = context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void transformText(String command, @Nullable MessageReceivedEvent event, boolean isWhisper) {
        Message message = null;
        Guild guild = null;
        List<Integer> quotations = findQuotations(command);
        String text;
        String args = null;

        if (quotations.isEmpty()) {
            text = command;
        } else if (quotations.size() == 2) {
            List<Integer> mentions = findOccurrences(command, "@");
            if (isWhisper && !mentions.isEmpty() && mentions.get(0) < quotations.get(0)) {
                args = command.substring(0, mentions.get(0));
            } else {
                args = command.substring(0, quotations.get(0));
            }
            text = command.substring(quotations.get(0) + 1, quotations.get(1));
        } else {
            throw new IllegalArgumentException("Invalid input. See " + DiscordListener.COMMAND_HELP);
        }

        StringBuilder responseBuilder = new StringBuilder();
        boolean randFormat = SettingsLoader.loadBoolProperty("RAND_FORMAT");
        boolean randEmojis = SettingsLoader.loadBoolProperty("RAND_EMOJIS");
        boolean replaceB = SettingsLoader.loadBoolProperty("REPLACE_B");
        boolean replaceWordPart = SettingsLoader.loadBoolProperty("REPLACE_WORDPART");

        if (args != null) {
            StringList argList = StringListImpl.createWords(args);

            // assert that all arguments are properly formatted (-arg)
            List<Integer> wordPositions = argList.getWordPositions();
            argList.assertThat(p -> p.valuesPrecededBy(wordPositions, "-"), "Invalid Argument. See " + DiscordListener.COMMAND_HELP);

            argList = argList.filterWords();
            if (argList.stream().allMatch(ARG_MAP::containsValue)) {
                if (argList.contains(ARG_MAP.get("RAND_FORMAT_ARG"))) {
                    randFormat = !randFormat;
                }
                if (argList.contains(ARG_MAP.get("RAND_EMOJIS_ARG"))) {
                    randEmojis = !randEmojis;
                }
                if (argList.contains(ARG_MAP.get("REPLACE_B_ARG"))) {
                    replaceB = !replaceB;
                }
                if (argList.contains(ARG_MAP.get("REPLACE_WORDPART_ARG"))) {
                    replaceWordPart = !replaceWordPart;
                }
            } else {
                throw new IllegalArgumentException("Invalid Argument. See " + DiscordListener.COMMAND_HELP);
            }
        }

        List<Emoji> emojis = context.getInstancesOf(Emoji.class);

        if (event != null) {
            guild = event.getGuild();
            message = event.getMessage();
            if (!isWhisper) {
                responseBuilder.append("**").append(message.getAuthor().getName()).append("**").append(System.lineSeparator());
            }
        }

        TextManipulationService manipulationService = new TextManipulationService(randFormat, randEmojis, replaceB, replaceWordPart, emojis, guild);
        responseBuilder.append(manipulationService.getOutput(text));

        if (isWhisper && message != null) {
            List<Member> mentionedMembers = message.getMentionedMembers();
            for (Member member : mentionedMembers) {
                alertService.send(responseBuilder.toString(), member.getUser());
            }
        } else {
            alertService.send(responseBuilder.toString(), message != null ? message.getChannel() : null);
        }
    }

    /**
     * saves new emojis to xml file
     * <p>
     * Creates new raw emoji if called with following syntax: e!add "emoji1, emoji2"
     * Creates new emoji with keywords or adjusts replace value for existing keyword: e!add "emoji1, emoji2" "keyword1, keyword2" "true, false"
     *
     * @param command whole command as String
     * @param channel Nullable; use if called from DiscordListener
     */
    public void saveEmojis(String command, @Nullable MessageChannel channel, @Nullable Guild guild) {
        List<Integer> quotations = findQuotations(command);
        int quotationSize = quotations.size();
        List<Integer> expectedQuotationSizes = ImmutableList.of(2, 4, 6, 8);
        validateQuotationSize(expectedQuotationSizes, quotationSize, channel);

        boolean commit = evaluateCommit(command, quotations);
        //for adding raw emojis with optional random tag (default value: true)
        if (quotationSize == 2 || quotationSize == 4) {
            StringList emojiValues = filterColons(StringListImpl
                .create(command.substring(quotations.get(0) + 1, quotations.get(1)), ",")
                .applyForEach(String::trim));
            StringList randomTags = StringListImpl.create();

            if (quotationSize == 4) {
                randomTags = StringListImpl
                    .createWords(command.substring(quotations.get(2) + 1, quotations.get(3)))
                    .filterWords();
            }

            instantiateEmojis(emojiValues, randomTags, Lists.newArrayList(), commit, guild, channel);
        }
        //for adding emojis with keywords with replace tags for the keywords and optional random tags for the emojis
        else if (quotationSize == 6 || quotationSize == 8) {
            int keywordsIndex = quotationSize == 8 ? 4 : 2;
            int replaceTagsIndex = quotationSize == 8 ? 6 : 4;
            List<KeywordBuilder> keywords = Lists.newArrayList();
            StringList randomTags = StringListImpl.create();

            StringList emojiValues = filterColons(StringListImpl
                .create(command.substring(quotations.get(0) + 1, quotations.get(1)), ",")
                .applyForEach(String::trim));

            StringList keywordValues = StringListImpl
                .createWords(command.substring(quotations.get(keywordsIndex) + 1, quotations.get(keywordsIndex + 1)))
                .filterWords();

            StringList replaceTags = StringListImpl
                .createWords(command.substring(quotations.get(replaceTagsIndex) + 1, quotations.get(replaceTagsIndex + 1)))
                .filterWords();

            if (quotations.size() == 8) {
                randomTags = StringListImpl
                    .createWords(command.substring(quotations.get(2) + 1, quotations.get(3)))
                    .filterWords();
            }

            keywordValues.assertUnique("Duplicate Keyword!");
            replaceTags.assertThat(p -> p.stream().allMatch(v -> v.equals("true") || v.equals("false")),
                "replace flags have to be either 'true' or 'false'");
            if (replaceTags.size() == 1) {
                boolean replace = Boolean.parseBoolean(replaceTags.get(0));
                for (String keywordValue : keywordValues) {
                    keywords.add(new KeywordBuilder().setKeywordValue(keywordValue).setReplace(replace));
                }
            } else if (replaceTags.size() == keywordValues.size()) {
                for (int i = 0; i < replaceTags.size(); i++) {
                    String keywordValue = keywordValues.get(i);
                    boolean replace = Boolean.parseBoolean(replaceTags.get(i));
                    keywords.add(new KeywordBuilder().setKeywordValue(keywordValue).setReplace(replace));
                }
            } else {
                throw new IllegalArgumentException("There has to be one replace flag for all emojis or one for each");
            }

            instantiateEmojis(emojiValues, randomTags, keywords, commit, guild, channel);
        }
    }

    private void instantiateEmojis(StringList emojiValues,
                                   StringList randomTags,
                                   List<KeywordBuilder> keywords,
                                   boolean commit,
                                   Guild guild,
                                   MessageChannel channel) {
        emojiValues.assertUnique("Duplicate Emoji!");

        Map<EmojiBuilder, Boolean> builders = new HashMap<>();
        for (String emojiValue : emojiValues) {
            EmojiBuilder builder = new EmojiBuilder();
            boolean discordEmoji = false;
            if (isDiscordEmoji(emojiValue, guild)) {
                discordEmoji = true;
                builder
                    .setValue(getDiscordEmojiValue(emojiValue, guild))
                    .setName(emojiValue)
                    .setGuildId(guild.getId())
                    .setGuildName(guild.getName());
            } else {
                builder.setValue(emojiValue);
            }
            builders.put(builder, discordEmoji);
        }

        if (!randomTags.isEmpty()) {
            randomTags.assertThat(p -> p.stream().allMatch(t -> t.equals("true") || t.equals("false")),
                "All random tags must be either 'true' or 'false'");

            if (randomTags.size() == 1) {
                builders.keySet().forEach(builder -> builder.setRandom(Boolean.parseBoolean(randomTags.get(0))));
            } else {
                if (randomTags.size() == emojiValues.size()) {
                    int i = 0;
                    for (EmojiBuilder builder : builders.keySet()) {
                        builder.setRandom(Boolean.parseBoolean(randomTags.get(i)));
                        ++i;
                    }
                } else {
                    throw new IllegalArgumentException("There has to be one random flag for each emoji, one in total or none at all");
                }
            }
        }

        if (!keywords.isEmpty()) {
            for (EmojiBuilder builder : builders.keySet()) {
                builder.addKeywords(keywords);
            }
        }

        if (commit && context.hasUncommittedTransactions()) {
            context.commitAll();
        }

        context.invoke(commit, () -> {
            for (EmojiBuilder builder : builders.keySet()) {
                Boolean isDiscordEmoji = builders.get(builder);
                if (isDiscordEmoji) {
                    builder.createDiscordEmoji(context);
                } else {
                    builder.createEmoji(context);
                }
            }
        }, channel);
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
        int quotationSize = quotations.size();
        List<Integer> expectedQuotationSizes = ImmutableList.of(2, 4);
        validateQuotationSize(expectedQuotationSizes, quotationSize, channel);

        boolean commit = evaluateCommit(command, quotations);
        if (quotationSize == 2) {
            StringList emojiList = filterColons(StringListImpl
                .create(command.substring(quotations.get(0) + 1, quotations.get(1)), ",")
                .applyForEach(String::trim));

            if (guild != null) {
                for (int i = 0; i < emojiList.size(); i++) {
                    String emoji = emojiList.get(i);

                    if (isDiscordEmoji(emoji, guild)) {
                        emojiList.set(i, getDiscordEmojiValue(emoji, guild));
                    }
                }
            }

            if (commit && context.hasUncommittedTransactions()) {
                context.commitAll();
            }

            context.invoke(commit, () -> {
                for (String emoji : emojiList) {
                    XmlElement foundEmoji = context.getElement(emoji);
                    if (foundEmoji != null) {
                        foundEmoji.delete();
                    } else {
                        alertService.send("Emoji " + emoji + " not found", channel);
                    }
                }
            }, channel);
        } else if (quotationSize == 4) {
            StringList emojiList = filterColons(StringListImpl
                .create(command.substring(quotations.get(0) + 1, quotations.get(1)), ",")
                .applyForEach(String::trim));
            StringList keywordList = StringListImpl
                .createWords(command.substring(quotations.get(2) + 1, quotations.get(3)))
                .filterWords();

            if (guild != null) {
                for (int i = 0; i < emojiList.size(); i++) {
                    String emoji = emojiList.get(i);

                    if (isDiscordEmoji(emoji, guild)) {
                        emojiList.set(i, getDiscordEmojiValue(emoji, guild));
                    }
                }
            }

            if (commit && context.hasUncommittedTransactions()) {
                context.commitAll();
            }

            context.invoke(commit, () -> {
                for (String emoji : emojiList) {
                    XmlElement foundEmoji = context.getElement(emoji);
                    if (foundEmoji != null) {
                        for (String keyword : keywordList) {
                            XmlElement subElement = foundEmoji.getSubElement(keyword);
                            if (subElement != null) {
                                subElement.delete();
                            } else {
                                alertService.send("Keyword " + keyword + " not found on emoji " + emoji, channel);
                            }
                        }
                    } else {
                        alertService.send("Emoji " + emoji + " not found", channel);
                    }
                }
            }, channel);
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Invalid input.");
            if (channel != null) builder.append(" See " + DiscordListener.COMMAND_HELP);
            alertService.send(builder.toString(), channel);
        }
    }

    private boolean evaluateCommit(String command, List<Integer> quotations) {
        boolean commit = true;
        StringList args = StringListImpl.createWords(command.substring(0, quotations.get(0)));
        List<Integer> argPositions = args.getWordPositions();
        args.assertThat(p -> p.valuesPrecededBy(argPositions, "-"), "Invalid Argument. See " + DiscordListener.COMMAND_HELP);
        for (String arg : args.filterWords()) {
            if (arg.equals("noCommit")) {
                commit = false;
            } else {
                throw new IllegalArgumentException("Unknown argument " + arg);
            }
        }
        return commit;
    }

    private void validateQuotationSize(List<Integer> expectedQuotationSizes, int quotationSize, MessageChannel channel) {
        if (!expectedQuotationSizes.contains(quotationSize)) {
            StringBuilder builder = new StringBuilder();
            builder.append("Invalid input.");
            if (channel != null) builder.append(" See " + DiscordListener.COMMAND_HELP);
            throw new IllegalArgumentException(builder.toString());
        }
    }

    /**
     * Search for emojis or keywords.
     * Shows found emoji and lists its keywords or shows found keyword and lists all emojis it occurs on
     *
     * @param searchTerm matches value of keyword or emoji
     * @param channel Nullable; use if called from DiscordListener
     */
    public void searchQuery(String searchTerm, @Nullable MessageChannel channel) {
        StringBuilder responseBuilder = new StringBuilder();

        if (searchTerm.startsWith(":") && searchTerm.endsWith(":")) {
            searchTerm = searchTerm.substring(1, searchTerm.length() - 1);
        }

        List<Emoji> emojis = context.getInstancesOf(Emoji.class, DiscordEmoji.class);
        List<DiscordEmoji> discordEmojis = context.getInstancesOf(DiscordEmoji.class);

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
        List<Emoji> emojis = context.getInstancesOf(Emoji.class);

        Set<Emoji> duplicateEmojis = getDuplicateEmojis(emojis);
        Multimap<Emoji, Keyword> upperCaseKeywords = getUpperCaseKeywords(emojis);
        Multimap<Emoji, Keyword> duplicateKeywords = getDuplicateKeywords(emojis);

        if (duplicateEmojis.isEmpty() && duplicateKeywords.isEmpty() && upperCaseKeywords.isEmpty()) {
            alertService.send("No configuration errors found.", channel);
        } else {
            if (!duplicateEmojis.isEmpty()) {
                context.invoke(true, () -> {
                    PersistenceManager persistenceManager = (PersistenceManager) context.getPersistenceManager();
                    persistenceManager.mergeDuplicateEmojis(duplicateEmojis);
                }, channel);
                // check again for duplicate and upper case keywords after merging emojis since the emojis have changed
                emojis = context.getInstancesOf(Emoji.class);
                upperCaseKeywords = getUpperCaseKeywords(emojis);
                duplicateKeywords = getDuplicateKeywords(emojis);
            }

            if (!duplicateKeywords.isEmpty()) {
                // variables used in lambda must be final
                Multimap<Emoji, Keyword> finalDuplicateKeywords = duplicateKeywords;
                context.invoke(true, () -> {
                    PersistenceManager persistenceManager = (PersistenceManager) context.getPersistenceManager();
                    persistenceManager.mergeDuplicateKeywords(finalDuplicateKeywords);
                }, channel);
            }

            if (!upperCaseKeywords.isEmpty()) {
                // variables used in lambda must be final
                Multimap<Emoji, Keyword> finalUpperCaseKeywords = upperCaseKeywords;
                context.invoke(true, () -> {
                    PersistenceManager persistenceManager = (PersistenceManager) context.getPersistenceManager();
                    persistenceManager.handleUpperCaseKeywords(finalUpperCaseKeywords);
                }, channel);
                //might also result in duplicate keywords so reload (E, e -> e, e)
                emojis = context.getInstancesOf(Emoji.class);
                duplicateKeywords = getDuplicateKeywords(emojis);
            }

            if (!duplicateKeywords.isEmpty()) {
                // variables used in lambda must be final
                Multimap<Emoji, Keyword> finalDuplicateKeywords = duplicateKeywords;
                context.invoke(true, () -> {
                    PersistenceManager persistenceManager = (PersistenceManager) context.getPersistenceManager();
                    persistenceManager.mergeDuplicateKeywords(finalDuplicateKeywords);
                }, channel);
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

    private Set<Emoji> getDuplicateEmojis(List<Emoji> emojis) {
        Set<String> checkedEmojis = Sets.newHashSet();
        Set<Emoji> duplicateEmojis = Sets.newHashSet();

        for (Emoji emoji : emojis) {
            String emojiValue = emoji.getEmojiValue();

            //if emoji value already exists add it to duplicates but only once
            if (!checkedEmojis.contains(emojiValue)) {
                checkedEmojis.add(emojiValue);
            } else {
                duplicateEmojis.add(emoji);
            }
        }

        return duplicateEmojis;
    }

    private Multimap<Emoji, Keyword> getDuplicateKeywords(List<Emoji> emojis) {
        Multimap<Emoji, Keyword> emojisWithDuplicateKeywords = ArrayListMultimap.create();

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
                else if (!emojisWithDuplicateKeywords.containsKey(emoji)) {
                    emojisWithDuplicateKeywords.put(emoji, keyword);
                }
                //if emoji was already added check if the same particular keyword has already been added
                else if (emojisWithDuplicateKeywords.get(emoji).stream().noneMatch(k -> k.getKeywordValue().equals(keywordValue))) {
                    emojisWithDuplicateKeywords.put(emoji, keyword);
                }
            }
        }

        return emojisWithDuplicateKeywords;
    }

    private Multimap<Emoji, Keyword> getUpperCaseKeywords(List<Emoji> emojis) {
        Multimap<Emoji, Keyword> upperCaseKeywords = HashMultimap.create();

        for (Emoji emoji : emojis) {
            emoji.getKeywords().stream()
                .filter(k -> !k.getKeywordValue().equals(k.getKeywordValue().toLowerCase()))
                .forEach(k -> upperCaseKeywords.put(emoji, k));
        }

        return upperCaseKeywords;
    }

    private List<Integer> findQuotations(String input) {
        return findOccurrences(input, "\"");
    }

    private List<Integer> findOccurrences(String input, String s) {
        List<Integer> positions = Lists.newArrayList();
        for (int i = 0; (i = input.toLowerCase().indexOf(s, i)) >= 0; i++) {
            positions.add(i);
        }

        return positions;
    }

    private StringList filterColons(StringList emojis) {
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
