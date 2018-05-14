import com.google.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TextManipulationService {

    private final boolean randFormat;
    private final Multimap<String, String> emojiMap;
    private final List<String> emojiValues;

    private static List<String> wrappers = new ArrayList<String>(Arrays.asList("_", "*", "~", "```")){};

    public TextManipulationService(boolean randFormat, Multimap<String, String> emojiMap, List<String> emojiValues) {
        this.randFormat = randFormat;
        this.emojiMap = emojiMap;
        this.emojiValues = emojiValues;
    }


    public String getOutput(String input) {
        String[] words = input.split(" ");

        return buildOutput(words);
    }

    private String buildOutput(String[] words) {
        replaceWords(words);
        if (randFormat) applyRandomFormat(words);

        return replaceChars(String.join(" ", words));
    }

    private void replaceWords(String[] words) {
        for (int i = 0; i < words.length; i++) {
            String word = words[i].toLowerCase();
            if (emojiMap.containsKey(word)) {
                StringBuilder wordBuilder = new StringBuilder();
                wordBuilder.append(words[i]);
                for (String emoji : emojiMap.get(word)) {
                    wordBuilder.append(emoji);
                }
                words[i] = wordBuilder.toString();
            }
        }
    }

    private String replaceChars(String input) {
        String[] letters = input.split("");

        for(int e = 0; e < letters.length; e++) {
            if (letters[e].equalsIgnoreCase("b")) {
                letters[e] = "\uD83C\uDD71️";
            }

            if (letters[e].equals(" ")) {
                int rand = ThreadLocalRandom.current().nextInt(0, emojiValues.size());
                letters[e] = " " + emojiValues.get(rand) + " ";
            }
        }

        return String.join("", letters);
    }

    private void applyRandomFormat(String[] words) {
        for (int i = 0; i < words.length; i++) {
            int rand = ThreadLocalRandom.current().nextInt(0, wrappers.size());
            String randomWrapper = wrappers.get(rand);
            words[i] = randomWrapper + words[i] + randomWrapper;
        }
    }

}
