import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Transform given input
 * wraps words in WhatsApp formatting wrappers if randFormat is true
 * adds random emoji between words
 * replaces keywords with emojis
 */
public class TextManipulationService {

    private final boolean randFormat;
    private final List<Emoji> emojis;

    private static List<String> wrappers = new ArrayList<String>(Arrays.asList("_", "*", "~", "```")){};

    public TextManipulationService(boolean randFormat, List<Emoji> emojis) {
        this.randFormat = randFormat;
        this.emojis = emojis;
    }


    public String getOutput(String input) {
        String[] words = input.split(" ");

        return buildOutput(words);
    }

    private String buildOutput(String[] words) {
        replaceWords(words);
        if (randFormat) applyRandomFormat(words);

        return filterOutput(String.join(" ", words));
    }

    private void replaceWords(String[] words) {
        List<Keyword> keywords = Emoji.getAllKeywords(emojis);
        List<String> keywordValues = Keyword.getAllKeywordValues(keywords);

        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (keywordValues.contains(word)) {
                StringBuilder wordBuilder = new StringBuilder();
                List<Keyword> selectedKeywords = Keyword.getSelectedKeywords(keywords, word);
                List<Emoji> selectedEmojis = Emoji.loadFromKeyword(selectedKeywords, emojis);

                if(!isReplace(selectedKeywords)) {
                    wordBuilder.append(words[i]);
                }

                for (Emoji emoji : selectedEmojis) {
                    wordBuilder.append(emoji.getEmojiValue());
                }

                words[i] = wordBuilder.toString();
            }
        }
    }

    //allMatch: replace if all Keywords true, anyMath: replace if one true
    private boolean isReplace(List<Keyword> keywords) {
        return keywords.stream().allMatch(Keyword::isReplace);
    }

    private String filterOutput(String input) {
        String[] chars = input.split("");

        for(int e = 0; e < chars.length; e++) {
            if (chars[e].equalsIgnoreCase("b")) {
                chars[e] = "\uD83C\uDD71️";
            }

            if (chars[e].equals(" ")) {
                int rand = ThreadLocalRandom.current().nextInt(0, emojis.size());
                chars[e] = " " + emojis.get(rand).getEmojiValue() + " ";
            }
        }

        return String.join("", chars);
    }

    private void applyRandomFormat(String[] words) {
        for (int i = 0; i < words.length; i++) {
            int rand = ThreadLocalRandom.current().nextInt(0, wrappers.size());
            String randomWrapper = wrappers.get(rand);
            words[i] = randomWrapper + words[i] + randomWrapper;
        }
    }

}
