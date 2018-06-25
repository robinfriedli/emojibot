package util;

import api.StringListImpl;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.entities.MessageChannel;

import javax.annotation.Nullable;
import java.util.List;

public class AlertService {

    public void alertRemovedEmojis(List<String> removedEmojis, List<String> missingEmojis, @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (!removedEmojis.isEmpty()) {
            builder.append("emojis removed: ").append(StringListImpl.create(removedEmojis).toSeparatedString())
                    .append(System.lineSeparator());
        }

        if (!missingEmojis.isEmpty()) {
            builder.append("emojis ").append(StringListImpl.create(missingEmojis).toSeparatedString()).append(" not found")
                    .append(System.lineSeparator());
        }

       send(builder.toString(), channel);
    }

    public void alertRemovedKeywords(List<String> missingEmojis,
                                     Multimap<String, String> removedKeywords,
                                     Multimap<String, String> missingKeywords,
                                     @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (!missingEmojis.isEmpty()) {
            builder.append("emojis not found: ").append(StringListImpl.create(missingEmojis).toSeparatedString())
                    .append(System.lineSeparator());
        }

        if (!removedKeywords.isEmpty()) {
            for (String emoji : removedKeywords.keySet()) {
                builder.append("keywords ").append(StringListImpl.create(removedKeywords.get(emoji)).toSeparatedString())
                        .append(" removed from emoji ").append(emoji)
                        .append(System.lineSeparator());
            }
        }

        if (!missingKeywords.isEmpty()) {
            for (String emoji : missingKeywords.keySet()) {
                builder.append("keywords ").append(StringListImpl.create(missingKeywords.get(emoji)).toSeparatedString())
                        .append(" not found on emoji ").append(emoji)
                        .append(System.lineSeparator());
            }
        }

        send(builder.toString(), channel);
    }

    public void alertAddedEmojis(List<String> addedEmojis, List<String> existingEmojis, @Nullable MessageChannel channel) {
        send(buildAddedEmojisAlertMessage(addedEmojis, existingEmojis), channel);
    }

    public void alertAddedEmojis(List<String> addedEmojis,
                                 List<String> existingEmojis,
                                 Multimap<String, String> addedKeywords,
                                 Multimap<String, String> adjustedKeywords,
                                 Multimap<String, String> existingKeywords,
                                 @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (addedKeywords.isEmpty() && adjustedKeywords.isEmpty()) {
            builder.append("All emojis and keywords already exist as is. No changes have been made.");
        } else {
            builder.append(buildAddedEmojisAlertMessage(addedEmojis, existingEmojis));

            if (!addedKeywords.isEmpty()) {
                for (String emoji : addedKeywords.keySet()) {
                    builder.append("keywords ").append(StringListImpl.create(addedKeywords.get(emoji)).toSeparatedString())
                            .append(" added to emoji ").append(emoji)
                            .append(System.lineSeparator());
                }
            }

            if (!adjustedKeywords.isEmpty()) {
                for (String emoji : adjustedKeywords.keySet()) {
                    builder.append("replace value of keywords ").append(StringListImpl.create(adjustedKeywords.get(emoji)))
                            .append(" adjusted on emoji ").append(emoji)
                            .append(System.lineSeparator());
                }
            }

            if (!existingKeywords.isEmpty()) {
                for (String emoji : existingKeywords.keySet()) {
                    builder.append("keywords ").append(StringListImpl.create(existingKeywords.get(emoji)).toSeparatedString())
                            .append(" already exist on emoji ").append(emoji)
                            .append(System.lineSeparator());
                }
            }
        }

        send(builder.toString(), channel);
    }

    public void alertMergedEmojis(List<String> mergedEmojis, @Nullable MessageChannel channel) {
        String message = String.format("duplicate emojis: %s merged", StringListImpl.create(mergedEmojis).toSeparatedString());

        send(message, channel);
    }

    public void alertMergedKeywords(Multimap<String, String> emojisWithDuplicateKeywords, @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        for (String emoji : emojisWithDuplicateKeywords.keySet()) {
            builder.append("Keywords merged on emoji ").append(emoji).append(": ")
                    .append(StringListImpl.create(emojisWithDuplicateKeywords.get(emoji)).toSeparatedString())
                    .append(System.lineSeparator());
        }

        send(builder.toString(), channel);
    }

    public void alertUpperCaseKeywords(List<String> upperCaseKeywords, @Nullable MessageChannel channel) {
        String message = "Keywords: "
                + StringListImpl.create(upperCaseKeywords).toSeparatedString()
                + " changed to lower case";

        send(message, channel);
    }

    private String buildAddedEmojisAlertMessage(List<String> addedEmojis, List<String> existingEmojis) {
        StringBuilder builder = new StringBuilder();

        if (!addedEmojis.isEmpty()) {
            builder.append("emojis added: ").append(StringListImpl.create(addedEmojis).toSeparatedString())
                    .append(System.lineSeparator());

            if (!existingEmojis.isEmpty()) {
                builder.append("emojis already exist: ").append(StringListImpl.create(existingEmojis).toSeparatedString())
                        .append(System.lineSeparator());
            }
        } else {
            builder.append("All emojis already exist.");
        }

        return builder.toString();
    }

    private void send(String message, @Nullable MessageChannel channel) {
        if (channel != null) {
            channel.sendMessage(message).queue();
        } else {
            System.out.print(message);
        }
    }

}
