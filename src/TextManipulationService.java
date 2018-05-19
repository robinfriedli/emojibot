import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

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

    private static List<String> wrappers = new ArrayList<>(Arrays.asList("_", "*", "~", "```"));

    public TextManipulationService(boolean randFormat, List<Emoji> emojis) {
        this.randFormat = randFormat;
        this.emojis = emojis;
        this.keywords = Emoji.getAllKeywords(emojis);
    }


    public String getOutput(String input) {
        input = applyKeywords(input);
        if (randFormat) input = applyRandomFormat(input.split(" "));

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
        List<String> handledKeywords = new ArrayList<>();

        for (Keyword keyword : keywords) {
            String keywordValue = keyword.getKeywordValue();
            if (!handledKeywords.contains(keywordValue) && input.toLowerCase().contains(keywordValue)) {
                //check if all Keywords of that value are isReplace
                if (Keyword.getSelectedKeywords(keywords, keywordValue).stream().allMatch(Keyword::isReplace)) {
                    input = replaceKeyword(true, input, keywordValue, keyword);
                } else {
                    for (String word : loadDifferentWords(input, keywordValue)) {
                        input = replaceKeyword(false, input, word, keyword);
                    }
                }
                handledKeywords.add(keywordValue);
            }
        }

        return input;
    }

    private String replaceKeyword(boolean replace, String input, String word, Keyword keyword) {
        StringBuilder builder = new StringBuilder();
        //if replace is false the word might not be lower case since the word gets loaded from the input so that capitalisation is not lost
        List<Integer> occurrences = findOccurrences(input, word.toLowerCase());

        //append input string up until the first keyword
        builder.append(input, 0, occurrences.get(0));
        for (int i = 0; i < occurrences.size(); i++) {
            //check if the keyword is part of a word
            if (isFullWord(input, occurrences.get(i), occurrences.get(i) + word.length())) {
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
                builder.append(input, occurrences.get(i) + word.length(), occurrences.get(i + 1));
            }
        }
        //append input string from last keyword
        builder.append(input, occurrences.get(occurrences.size() - 1) + word.length(), input.length());

        return builder.toString();
    }

    private boolean isFullWord(String input, int start, int end) {
        Pattern letters = Pattern.compile("[A-Za-zÀ-ÿ]");
        return (start == 0 || !letters.matcher(Character.toString(input.charAt(start - 1))).matches())
                && (end == input.length() || !letters.matcher(Character.toString(input.charAt(end))).matches());
    }

    /**
     * Load different words from input matching keyword
     * Since they match the keyword they only differ in capitalisation
     * For in case the same keyword exist several times with different capitalisation
     *
     * @param input
     * @param keywordValue
     * @return words
     */
    private List<String> loadDifferentWords(String input, String keywordValue) {
        List<Integer> wordPositions = findOccurrences(input, keywordValue);
        List<String> words = new ArrayList<>();
        for (int wordPosition : wordPositions) {
            String word = (String) input.subSequence(wordPosition, wordPosition + keywordValue.length());
            if (!words.contains(word) && isFullWord(input, wordPosition, wordPosition + word.length())) {
                words.add(word);
            }
        }

        return words;
    }

    /**
     * Find exactly where in the input string the keyword occurs for when I want to load the exact word from input
     * This is used when replace is false and the word should be written along with the emoji.
     * Instead of just writing the keyword value I load the word from the actual input so that capitalisation isn't lost
     *
     * @param input
     * @param keyword
     * @return positions of keyword
     */
    private List<Integer> findOccurrences(String input, String keyword) {
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; (i = input.toLowerCase().indexOf(keyword, i)) >= 0; i++) {
            positions.add(i);
        }

        return positions;
    }

    private String getEmojiString(Keyword keyword) {
        StringBuilder builder = new StringBuilder();
        List<Emoji> selectedEmojis = Emoji.loadFromKeyword(keyword, emojis);

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
            strings.replaceValueAt(pos, emojiSpace);
        }

        input = strings.toString();

        return input;
    }

    private String applyRandomFormat(String[] words) {
        for (int i = 0; i < words.length; i++) {
            int rand = ThreadLocalRandom.current().nextInt(0, wrappers.size());
            String randomWrapper = wrappers.get(rand);
            words[i] = randomWrapper + words[i] + randomWrapper;
        }

        return String.join(" ", words);
    }

}
