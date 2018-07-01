package core;

import api.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Transform given input
 * wraps words in WhatsApp formatting wrappers if randFormat is true
 * adds random emoji between words
 * replaces keywords with emojis if replace is true else adds emoji after word
 */
public class TextManipulationService {

    private final boolean randFormat;
    private final List<Emoji> emojis;
    private final List<Keyword> keywords;
    private final Guild guild;

    private static List<String> wrappersStart =
            ImmutableList.of("_", "**", "***", "__", "__*", "__**", "__***", "~~");
    private static List<String> wrappersEnd =
            ImmutableList.of("_", "**", "***", "__", "*__", "**__", "***__", "~~");

    public TextManipulationService(boolean randFormat, List<Emoji> emojis, Guild guild) {
        this.randFormat = randFormat;
        this.emojis = emojis;
        this.keywords = Emoji.getAllKeywords(emojis);
        this.guild = guild;
    }


    public String getOutput(String input) {
        input = applyKeywords(input);
        if (randFormat) input = applyRandomFormat(input);

        return filterOutput(input);
    }

    /**
     * loops over keywords and replaces occurrences in input
     * checks if keyword has already been handled
     * this is relevant if replace is false and there's several keywords with the same value
     *
     * @param input
     * @return modified input
     */
    private String applyKeywords(String input) {
        List<String> handledKeywords = Lists.newArrayList();

        for (Keyword keyword : keywords) {
            String keywordValue = keyword.getKeywordValue();
            if (!handledKeywords.contains(keywordValue) && input.toLowerCase().contains(keywordValue)) {
                //check if all Keywords of that value are isReplace
                if (Keyword.getSelectedKeywords(keywords, keywordValue).stream().allMatch(Keyword::isReplace)) {
                    input = replaceKeyword(true, input, keywordValue, keyword);
                } else {
                    input = replaceKeyword(false, input, keywordValue, keyword);
                }
                handledKeywords.add(keywordValue);
            }
        }

        return input;
    }

    private String replaceKeyword(boolean replace, String input, String keywordValue, Keyword keyword) {
        StringBuilder builder = new StringBuilder();
        List<Integer> occurrences = findOccurrences(input, keywordValue);

        //append input string up until the first keyword
        builder.append(input, 0, occurrences.get(0));
        for (int i = 0; i < occurrences.size(); i++) {
            //load exact word from input string so that capitalisation is not lost
            String word = (String) input.subSequence(occurrences.get(i), occurrences.get(i) + keywordValue.length());
            //check if the keyword is part of a word
            if (isFullWord(input, occurrences.get(i), occurrences.get(i) + keywordValue.length())) {
                if (replace) {
                    builder.append(getEmojiString(keyword));
                } else {
                    builder.append(word).append(getEmojiString(keyword));
                }
            } else {
                builder.append(word);
            }
            //append input string up until next keyword if not the last keyword
            if (i < occurrences.size() - 1) {
                builder.append(input, occurrences.get(i) + keywordValue.length(), occurrences.get(i + 1));
            }
        }
        //append input string from last keyword
        builder.append(input, occurrences.get(occurrences.size() - 1) + keywordValue.length(), input.length());

        return builder.toString();
    }

    private boolean isFullWord(String input, int start, int end) {
        Pattern letters = Pattern.compile("[A-Za-zÀ-ÿ]");
        return (start == 0 || !letters.matcher(Character.toString(input.charAt(start - 1))).matches())
                && (end == input.length() || !letters.matcher(Character.toString(input.charAt(end))).matches()
                && (start == 0 || !Character.toString(input.charAt(start - 1)).equals(":"))
                && !Character.toString(input.charAt(end)).equals(":"));
    }

    /**
     * Find where the keyword appears in the input string so the exact word can be loaded and capitalisation is not lost
     * when replacing
     *
     * @param input
     * @param keyword
     * @return positions of keyword
     */
    private List<Integer> findOccurrences(String input, String keyword) {
        List<Integer> positions = Lists.newArrayList();
        for (int i = 0; (i = input.toLowerCase().indexOf(keyword, i)) >= 0; i++) {
            positions.add(i);
        }

        return positions;
    }

    private String getEmojiString(Keyword keyword) {
        StringBuilder builder = new StringBuilder();
        List<Emoji> selectedEmojis = Emoji.loadFromKeyword(keyword, emojis);

        List<DiscordEmoji> discordEmojis = emojis.stream().filter(e -> e instanceof DiscordEmoji)
                .map(e -> (DiscordEmoji) e)
                .collect(Collectors.toList());
        List<DiscordEmoji> selectedDiscordEmojis = DiscordEmoji.getForKeyword(keyword, discordEmojis);

        for (DiscordEmoji selectedDiscordEmoji : selectedDiscordEmojis) {
            for (Emote emote : guild.getEmotesByName(selectedDiscordEmoji.getName(), true)) {
                builder.append(emote.getAsMention());
            }
        }

        for (Emoji selectedEmoji : selectedEmojis) {
            builder.append(selectedEmoji.getEmojiValue());
        }

        return builder.toString();
    }

    private String filterOutput(String input) {
        //replace all b or B with B emoji
        input = input.replaceAll("(?i)b", "\uD83C\uDD71️");

        //replace all spaces with emojis
        StringList strings = StringListImpl.charsToList(input);

        List<Integer> spacePositions = findOccurrences(input, " ");
        for (int pos : spacePositions) {
            int rand = ThreadLocalRandom.current().nextInt(0, emojis.size());
            String emojiSpace = String.format(" %s ", emojis.get(rand).getEmojiValue());
            strings.set(pos, emojiSpace);
        }

        input = strings.toString();

        return input;
    }

    private String applyRandomFormat(String input) {
        StringList words = StringListImpl.createWords(input);

        for (int i = 0; i < words.size(); i++) {
            //list also contains spaces and interpunctions so we need to check if this is indeed a word
            String prevWord = i > 0 ? words.get(i - 1) : " ";
            String nextWord = i < words.size() - 1 ? words.get(i + 1) : " ";
            String word = words.get(i);
            if (isWord(prevWord, word, nextWord)) {
                int rand = ThreadLocalRandom.current().nextInt(0, wrappersStart.size());
                String randomWrapperStart = wrappersStart.get(rand);
                String randomWrapperEnd = wrappersEnd.get(rand);

                word = randomWrapperStart + word + randomWrapperEnd;
                words.set(i, word);
            }
        }

        return words.toString();
    }

    private boolean isWord(String prevWord, String word, String nextWord) {
        Character[] chars = word.chars().mapToObj(c -> (char) c).toArray(Character[]::new);
        //check that the word is not surrounded by ':' in which case it's most likely a discord emoji
        return Arrays.stream(chars).allMatch(Character::isLetter)
                && !(prevWord.equals(":") && nextWord.equals(":"));
    }

}
