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
    private final Multimap<String, String> emojiMap;

    private static List<String> wrappers = new ArrayList<String>(Arrays.asList("_", "*", "~", "```")){};

    public TextManipulationService(boolean randFormat, Multimap<String, String> emojiMap) {
        this.randFormat = randFormat;
        this.emojiMap = emojiMap;
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
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (emojiMap.containsKey(word)) {
                StringBuilder wordBuilder = new StringBuilder();
                wordBuilder.append(words[i]);
                //the same keyword can be set on different emojis, add all of them
                for (String emoji : emojiMap.get(word)) {
                    wordBuilder.append(emoji);
                }
                words[i] = wordBuilder.toString();
            }
        }
    }

    private String filterOutput(String input) {
        String[] chars = input.split("");

        for(int e = 0; e < chars.length; e++) {
            if (chars[e].equalsIgnoreCase("b")) {
                chars[e] = "\uD83C\uDD71️";
            }

            if (chars[e].equals(" ")) {
                int rand = ThreadLocalRandom.current().nextInt(0, EmojiLoadingService.EMOJIS.size());
                chars[e] = " " + EmojiLoadingService.EMOJIS.get(rand) + " ";
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
