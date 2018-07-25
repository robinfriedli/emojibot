package util;

import api.StringList;
import api.StringListImpl;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

public class AlertService {

    private static final int MESSAGE_LENGTH_LIMIT = 1000;

    @Deprecated
    public void alertRemovedEmojis(List<String> removedEmojis, List<String> missingEmojis, @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (!removedEmojis.isEmpty()) {
            builder.append("emojis removed: ").append(StringListImpl.create(removedEmojis).toSeparatedString(", "))
                .append(System.lineSeparator());
        }

        if (!missingEmojis.isEmpty()) {
            builder.append("emojis ").append(StringListImpl.create(missingEmojis).toSeparatedString(", ")).append(" not found")
                .append(System.lineSeparator());
        }

        send(builder.toString(), channel);
    }

    @Deprecated
    public void alertRemovedKeywords(List<String> missingEmojis,
                                     Multimap<String, String> removedKeywords,
                                     Multimap<String, String> missingKeywords,
                                     @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (!missingEmojis.isEmpty()) {
            builder.append("emojis not found: ").append(StringListImpl.create(missingEmojis).toSeparatedString(", "))
                .append(System.lineSeparator());
        }

        if (!removedKeywords.isEmpty()) {
            for (String emoji : removedKeywords.keySet()) {
                builder.append("keywords ").append(StringListImpl.create(removedKeywords.get(emoji)).toSeparatedString(", "))
                    .append(" removed from emoji ").append(emoji)
                    .append(System.lineSeparator());
            }
        }

        if (!missingKeywords.isEmpty()) {
            for (String emoji : missingKeywords.keySet()) {
                builder.append("keywords ").append(StringListImpl.create(missingKeywords.get(emoji)).toSeparatedString(", "))
                    .append(" not found on emoji ").append(emoji)
                    .append(System.lineSeparator());
            }
        }

        send(builder.toString(), channel);
    }

    @Deprecated
    public void alertAddedEmojis(List<String> addedEmojis,
                                 List<String> existingEmojis,
                                 List<String> adjustedEmojis,
                                 @Nullable MessageChannel channel) {
        send(buildAddedEmojisAlertMessage(addedEmojis, existingEmojis, adjustedEmojis), channel);
    }

    @Deprecated
    public void alertAddedEmojis(List<String> addedEmojis,
                                 List<String> existingEmojis,
                                 List<String> adjustedEmojis,
                                 Multimap<String, String> addedKeywords,
                                 Multimap<String, String> adjustedKeywords,
                                 Multimap<String, String> existingKeywords,
                                 @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        if (addedKeywords.isEmpty() && adjustedKeywords.isEmpty()) {
            builder.append("All emojis and keywords already exist as is. No changes have been made.")
                .append(System.lineSeparator());
        } else {
            builder.append(buildAddedEmojisAlertMessage(addedEmojis, existingEmojis, adjustedEmojis));

            if (!addedKeywords.isEmpty()) {
                for (String emoji : addedKeywords.keySet()) {
                    builder.append("keywords ").append(StringListImpl.create(addedKeywords.get(emoji)).toSeparatedString(", "))
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
                    builder.append("keywords ").append(StringListImpl.create(existingKeywords.get(emoji)).toSeparatedString(", "))
                        .append(" already exist on emoji ").append(emoji)
                        .append(System.lineSeparator());
                }
            }
        }

        send(builder.toString(), channel);
    }

    public void alertMergedEmojis(List<String> mergedEmojis, @Nullable MessageChannel channel) {
        String message = String.format("duplicate emojis: %s merged", StringListImpl.create(mergedEmojis).toSeparatedString(", "));

        send(message, channel);
    }

    public void alertMergedKeywords(Multimap<String, String> emojisWithDuplicateKeywords, @Nullable MessageChannel channel) {
        StringBuilder builder = new StringBuilder();

        for (String emoji : emojisWithDuplicateKeywords.keySet()) {
            builder.append("Keywords merged on emoji ").append(emoji).append(": ")
                .append(StringListImpl.create(emojisWithDuplicateKeywords.get(emoji)).toSeparatedString(", "))
                .append(System.lineSeparator());
        }

        send(builder.toString(), channel);
    }

    public void alertUpperCaseKeywords(List<String> upperCaseKeywords, @Nullable MessageChannel channel) {
        String message = "Keywords: "
            + StringListImpl.create(upperCaseKeywords).toSeparatedString(", ")
            + " changed to lower case";

        send(message, channel);
    }

    private String buildAddedEmojisAlertMessage(List<String> addedEmojis,
                                                List<String> existingEmojis,
                                                List<String> adjustedEmojis) {
        StringBuilder builder = new StringBuilder();

        if (!addedEmojis.isEmpty() || !adjustedEmojis.isEmpty()) {
            if (!addedEmojis.isEmpty()) {
                builder.append("emojis added: ").append(StringListImpl.create(addedEmojis).toSeparatedString(", "))
                    .append(System.lineSeparator());
            }

            if (!existingEmojis.isEmpty()) {
                builder.append("emojis already exist: ").append(StringListImpl.create(existingEmojis).toSeparatedString(", "))
                    .append(System.lineSeparator());
            }

            if (!adjustedEmojis.isEmpty()) {
                builder.append("random flag adjusted on emojis: ").append(StringListImpl.create(adjustedEmojis).toString())
                    .append(System.lineSeparator());
            }
        } else {
            builder.append("All emojis already exist as is.").append(System.lineSeparator());
        }

        return builder.toString();
    }

    public void send(String message, @Nullable MessageChannel channel) {
        if (channel != null) {
            if (message.length() < MESSAGE_LENGTH_LIMIT) {
                channel.sendMessage(message).queue();
            } else {
                List<String> outputParts = separateMessage(message);
                outputParts.forEach(part -> channel.sendMessage(part).queue());
            }
        } else {
            System.out.println(message);
        }
    }

    public void send(String message, User user) {
        if (message.length() < MESSAGE_LENGTH_LIMIT) {
            user.openPrivateChannel().queue(channel -> channel.sendMessage(message).queue());
        } else {
            List<String> outputParts = separateMessage(message);
            outputParts.forEach(part -> user.openPrivateChannel().queue(channel -> channel.sendMessage(part).queue()));
        }
    }

    private List<String> separateMessage(String message) {
        List<String> outputParts = Lists.newArrayList();
        //first split the message into paragraphs (Lists.newArrayList() because Arrays.asList returns immutable list)
        StringList paragraphs = StringListImpl.separateString(message, "\n");

        for (int i = 0; i < paragraphs.size(); i++) {
            String paragraph = paragraphs.get(i);
            if (paragraph.length() + System.lineSeparator().length() < MESSAGE_LENGTH_LIMIT) {
                //check that paragraph is not an empty line
                if (notEmpty(paragraph)) {
                    if (i < paragraphs.size() - 1) paragraph = paragraph + System.lineSeparator();
                    outputParts.add(paragraph);
                }
            } else {
                //if the paragraph is too long separate into sentences
                StringList sentences = StringListImpl.separateString(paragraph, "\\. ");
                for (String sentence : sentences) {
                    //check if sentence is shorter than 2000 characters, else split into words
                    if (sentence.length() < MESSAGE_LENGTH_LIMIT) {
                        //since we don't want to send a message for each sentence fill the same part until full
                        outputParts = fillParts(outputParts, sentence);
                    } else {
                        //if sentence is longer than 2000 characters split into words
                        StringList words = StringListImpl.separateString(sentence, " ");

                        for (String word : words) {
                            if (word.length() < MESSAGE_LENGTH_LIMIT) {
                                outputParts = fillParts(outputParts, word);
                            } else {
                                //this should never happen since discord does not allow you to send messages longer than
                                // 2000 characters and if there are no spaces there are no emojis to make the text longer
                                StringList chars = StringListImpl.charsToList(word);
                                for (String charString : chars) {
                                    outputParts = fillParts(outputParts, charString);
                                }
                            }
                        }
                    }
                }
            }
        }

        return outputParts;
    }

    private boolean notEmpty(String paragraph) {
        Character[] chars = paragraph.chars().mapToObj(c -> (char) c).toArray(Character[]::new);
        return Arrays.stream(chars).anyMatch(Character::isLetter);
    }

    private List<String> fillParts(List<String> outputParts, String s) {
        if (outputParts.isEmpty()) outputParts.add("");
        int currentPart = outputParts.size() - 1;

        if (outputParts.get(currentPart).length() + s.length() < MESSAGE_LENGTH_LIMIT) {
            outputParts.set(currentPart, outputParts.get(currentPart) + s);
        } else {
            outputParts.add(s);
        }

        return outputParts;
    }

}
