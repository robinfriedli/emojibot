package util;

import api.Emoji;
import api.Keyword;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import core.EmojiAddingService;
import core.EmojiLoadingService;
import net.dv8tion.jda.core.entities.MessageChannel;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class CommandHandler {

    private AlertService alertService = new AlertService();

    /**
     * saves new emojis to xml file
     * <p>
     * Creates new raw emoji if called with following syntax: +e "emoji1, emoji2"
     * Creates new emoji with keywords or adjusts replace value for existing keyword: +e "emoji1, emoji2" "keyword1, keyword2" "true, false"
     *
     * @param command whole command as String
     * @param channel Nullable; use if called from DiscordListener
     */
    public void saveEmojis(String command, @Nullable MessageChannel channel) {
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        List<Integer> quotations = findQuotations(command);

        //if following syntax is used: +e "emoji1, emoji2"
        if (quotations.size() == 2) {
            String emojis = command.substring(command.indexOf("\"") + 1, command.lastIndexOf("\""));
            String[] emojiList = emojis.split(", ");

            emojiAddingService.addEmojis(emojiList, channel);
        }

        //if following syntax is used: +e "emoji1, emoji2" "keyword1, keyword2" "true, false"
        else if (quotations.size() == 6) {
            String emojis = command.substring(quotations.get(0) + 1, quotations.get(1));
            String keywords = command.substring(quotations.get(2) + 1, quotations.get(3));
            String replaceTags = command.substring(quotations.get(4) + 1, quotations.get(5));

            String[] emojiList = emojis.split(", ");
            String[] keywordList = keywords.split(", ");
            String[] replaceTagList = replaceTags.split(", ");

            if (Arrays.stream(replaceTagList).allMatch(s -> s.equals("true") || s.equals("false"))
                    && keywordList.length == replaceTagList.length
                    && Arrays.stream(keywordList).allMatch(k -> k.equals(k.toLowerCase()))) {
                try {
                    emojiAddingService.addEmojis(emojiList, keywordList, replaceTagList, channel);
                } catch (IllegalStateException e) {
                    channel.sendMessage(e.getMessage()).queue();
                }
            } else {
                StringBuilder builder = new StringBuilder();

                builder.append("There has to be one replace flag for each keyword").append(System.lineSeparator())
                        .append("Replace tag has to be either 'true' or 'false'").append(System.lineSeparator())
                        .append("Keywords have to be lower case").append(System.lineSeparator());

                if (channel != null) builder.append("See 'e!help'");

                alertService.send(builder.toString(), channel);
            }
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Invalid input.");
            if (channel != null) builder.append(" See 'e!help'");
            alertService.send(builder.toString(), channel);
        }
    }

    /**
     * deletes emoji when called with following syntax: -e "emoji1, emoji2"
     * deletes keywords from specified emojis: -e "emoji1, emoji2" "keyword1, keyword2"
     *
     * @param command whole command as String
     * @param channel Nullable; use if called from DiscordListener
     */
    public void deleteEmojis(String command, @Nullable MessageChannel channel) {
        EmojiAddingService emojiAddingService = new EmojiAddingService();
        List<Integer> quotations = findQuotations(command);

        if (quotations.size() == 2) {
            String emojiStrings = command.substring(command.indexOf("\"") + 1, command.lastIndexOf("\""));
            List<String> emojiList = Lists.newArrayList(Arrays.asList(emojiStrings.split(", ")));

            emojiAddingService.removeEmojis(emojiList, channel);
        } else if (quotations.size() == 4) {
            String emojiStrings = command.substring(quotations.get(0) + 1, quotations.get(1));
            String keywords = command.substring(quotations.get(2) + 1, quotations.get(3));

            List<String> emojiList = Lists.newArrayList(Arrays.asList(emojiStrings.split(", ")));
            List<String> keywordList = Lists.newArrayList(Arrays.asList(keywords.split(", ")));

            emojiAddingService.removeKeywords(emojiList, keywordList, channel);
        } else {
            StringBuilder builder = new StringBuilder();
            builder.append("Invalid input.");
            if (channel != null) builder.append(" See 'e!help'");
            alertService.send(builder.toString(), channel);
        }
    }

    /**
     * Search for emojis or keywords.
     * Shows found emoji and lists its keywords or shows found keyword and lists all emojis it occurs on
     *
     * @param searchTerm matches value of keyword or emoji
     * @param channel    Nullable; use if called from DiscordListener
     */
    public void searchQuery(String searchTerm, @Nullable MessageChannel channel) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        StringBuilder responseBuilder = new StringBuilder();

        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        List<Keyword> keywords = Emoji.getAllKeywords(emojis);

        Optional<Emoji> optionalEmoji = emojis.stream().filter(e -> e.getEmojiValue().equals(searchTerm)).findAny();
        Optional<Keyword> optionalKeyword = keywords.stream().filter(k -> k.getKeywordValue().equals(searchTerm)).findAny();

        if (optionalEmoji.isPresent()) {
            responseBuilder.append("\"").append(searchTerm).append("\"").append(" is an emoji.");
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
            responseBuilder.append("\"").append(searchTerm).append("\"").append(" is a keyword for following emojis:")
                    .append(System.lineSeparator());
            List<Emoji> emojisForKeyword = Emoji.loadFromKeyword(optionalKeyword.get(), emojis);

            for (Emoji emoji : emojisForKeyword) {
                responseBuilder.append(emoji.getEmojiValue())
                        .append("\treplace: ").append(emoji.requireKeyword(searchTerm).isReplace())
                        .append(System.lineSeparator());
            }
        }

        if (!(optionalEmoji.isPresent() || optionalKeyword.isPresent())) {
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

    private Set<String> getDuplicateEmojis(List<Emoji> emojis) {
        Set<String> checkedEmojis = Sets.newHashSet();
        Set<String> duplicateEmojis = Sets.newHashSet();

        for (Emoji emoji : emojis) {
            String emojiValue = emoji.getEmojiValue();

            //if emoji value already exists add it to duplicates but only once
            if (!checkedEmojis.contains(emojiValue)) {
                checkedEmojis.add(emojiValue);
            } else duplicateEmojis.add(emojiValue);
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

}