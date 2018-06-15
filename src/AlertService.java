import net.dv8tion.jda.core.entities.MessageChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AlertService {

    public void alertMissingEmojis(List<String> missingEmojis, MessageChannel channel) {
        channel.sendMessage("emojis " + StringListImpl.create(missingEmojis).toSeparatedString()
                + " not found and ignored.").queue();
    }

    public void alertRemovedEmojis(List<String> removedEmojis, MessageChannel channel) {
        channel.sendMessage("emojis " + StringListImpl.create(removedEmojis).toSeparatedString() + " were removed").queue();
    }

    public void alertAddedEmojis(String[] emojis, List<Emoji> allEmojis, MessageChannel channel) {
        channel.sendMessage(buildAddedEmojisAlertMessage(emojis, allEmojis)).queue();
    }

    public void alertAddedEmojis(String[] emojis,
                                 String[] keywords,
                                 String[] replaceTags,
                                 List<Emoji> allEmojis,
                                 MessageChannel channel) {
        StringBuilder builder = new StringBuilder();
        builder.append(buildAddedEmojisAlertMessage(emojis, allEmojis));
        builder.append(System.lineSeparator());

        //incrementing index to load the replaceTag matching the keyword
        int indexForReplaceTag = 0;
        for (String keyword : keywords) {
            //emojis where the keyword doesn't exist yet
            List<String> emojisAdded = new ArrayList<>();
            //emojis where the keyword exists with a different replace value
            List<String> emojisAdjusted = new ArrayList<>();
            //emojis where the keyword already exists as is
            List<String> emojisExist = new ArrayList<>();

            for (String emoji : emojis) {
                Emoji emojiObj = Emoji.loadFromValue(emoji, allEmojis);
                if (emojiObj != null) {
                    Keyword keywordObj = emojiObj.getKeyword(keyword);

                    if (keywordObj == null) {
                        emojisAdded.add(emoji);
                    } else {
                        if (keywordObj.isReplace() != Boolean.parseBoolean(replaceTags[indexForReplaceTag])) {
                            emojisAdjusted.add(emoji);
                        } else {
                            emojisExist.add(emoji);
                        }
                    }
                } else {
                    emojisAdded.add(emoji);
                }
            }

            if (!emojisAdded.isEmpty()) {
                builder.append("keyword ").append(keyword).append(" added to emojis: ")
                        .append(StringListImpl.create(emojisAdded).toSeparatedString())
                        .append(System.lineSeparator());
            }

            if (!emojisAdjusted.isEmpty()) {
                builder.append("keyword ").append(keyword).append("'s replace value adjusted on emojis: ")
                        .append(StringListImpl.create(emojisAdjusted).toSeparatedString())
                        .append(System.lineSeparator());
            }

            if (!emojisExist.isEmpty()) {
                builder.append("keyword ").append(keyword).append(" already exists on emojis: ")
                        .append(StringListImpl.create(emojisExist).toSeparatedString())
                        .append(System.lineSeparator());
            }

            ++indexForReplaceTag;
        }

        channel.sendMessage(builder.toString()).queue();
    }

    public void alertRemovedKeywords(List<String> keywords,
                                     List<String> emojiStrings,
                                     List<Emoji> emojis,
                                     MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        for (String emoji : emojiStrings) {
            if (keywords.stream().anyMatch(k -> keywordExists(k, emoji, emojis))) {
                builder.append("keywords: ");

                for (int i = 0; i < keywords.size(); i++) {
                    if (keywordExists(keywords.get(i), emoji, emojis)) {
                        builder.append(keywords.get(i));
                        if (i < keywords.size() - 1) builder.append(", ");
                    }
                }

                builder.append(" removed from emoji: ").append(emoji);
                builder.append(System.lineSeparator());
            }
        }

        channel.sendMessage(builder.toString()).queue();
    }

    public void alertMissingKeyword(String keyword,
                                    List<String> selectedEmojis,
                                    List<Emoji> emojis,
                                    MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (selectedEmojis.stream().anyMatch(e -> !keywordExists(keyword, e, emojis))) {
            builder.append("keyword ").append(keyword).append(" does not exists on emojis ");

            for (int i = 0; i < selectedEmojis.size(); i++) {
                if (!keywordExists(keyword, selectedEmojis.get(i), emojis)) {
                    builder.append(selectedEmojis.get(i));
                    if (i < selectedEmojis.size() - 1) builder.append(", ");
                }
            }
        }

        channel.sendMessage(builder.toString()).queue();
    }

    public boolean emojiExists(String selectedEmoji, List<Emoji> emojis) {
        return emojis.stream().anyMatch(e -> e.getEmojiValue().equals(selectedEmoji));
    }

    public boolean keywordExists(String selectedKeyword, String selectedEmoji, List<Emoji> emojis) {
        Optional<Emoji> optionalEmoji = emojis.stream().filter(e -> e.getEmojiValue().equals(selectedEmoji)).findAny();

        if (optionalEmoji.isPresent()) {
            Emoji emoji = optionalEmoji.get();
            return emoji.getKeywords().stream().anyMatch(k -> k.getKeywordValue().equals(selectedKeyword));
        } else {
            return false;
        }
    }

    private String buildAddedEmojisAlertMessage(String[] emojis, List<Emoji> allEmojis) {
        StringBuilder builder = new StringBuilder();

        if (Arrays.stream(emojis).anyMatch(e -> !emojiExists(e, allEmojis))) {
            builder.append("emojis added: ");
            List<String> addedEmojis = new ArrayList<>();
            List<String> existingEmojis = new ArrayList<>();

            for (String emoji : emojis) {
                if (!emojiExists(emoji, allEmojis)) {
                    addedEmojis.add(emoji);
                } else {
                    existingEmojis.add(emoji);
                }
            }

            builder.append(StringListImpl.create(addedEmojis).toSeparatedString());

            if (!existingEmojis.isEmpty()) {
                builder.append(System.lineSeparator()).append("emojis already exist: ");
                builder.append(StringListImpl.create(existingEmojis).toSeparatedString());
            }
        } else {
            builder.append("all emojis already exist");
        }

        return builder.toString();
    }

}
