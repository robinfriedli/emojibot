package util;

import java.util.List;
import java.util.stream.Collectors;

import api.Keyword;
import api.StringList;
import api.StringListImpl;
import core.Context;
import core.EmojiChangingEvent;
import core.Event;
import core.KeywordChangingEvent;
import core.UpperCaseKeywordEvent;
import net.dv8tion.jda.core.entities.MessageChannel;

public class AlertEventListener extends EventListener {

    private final AlertService alertService;
    private final Context context;

    public AlertEventListener(AlertService alertService, Context context) {
        this.alertService = alertService;
        this.context = context;
    }

    @Override
    public void emojiCreating(Event event) {
        MessageChannel channel = context.getChannel();
        alertService.send("Emoji added: " + event.getSource().getEmojiValue(), channel);
    }

    @Override
    public void emojiDeleting(Event event) {
        MessageChannel channel = context.getChannel();
        alertService.send("Emoji deleted: " + event.getSource().getEmojiValue(), channel);
    }

    @Override
    public void emojiChanging(EmojiChangingEvent event) {
        MessageChannel channel = context.getChannel();
        StringBuilder responseBuilder = new StringBuilder();

        Boolean randomTag = event.getRandomTag();
        List<Keyword> addedKeywords = event.getAddedKeywords();
        List<Keyword> removedKeywords = event.getRemovedKeywords();
        List<KeywordChangingEvent> changedKeywords = event.getChangedKeywords();

        if (randomTag != null
            || (addedKeywords != null && !addedKeywords.isEmpty())
            || (removedKeywords != null && !removedKeywords.isEmpty())
            || (changedKeywords != null && !changedKeywords.isEmpty())) {

            responseBuilder.append("Emoji changed: ").append(event.getSource().getEmojiValue()).append(System.lineSeparator());

            if (randomTag != null) {
                responseBuilder.append("\tRandom flag changed to ").append(randomTag).append(System.lineSeparator());
            }

            if (addedKeywords != null && !addedKeywords.isEmpty()) {
                StringList keywordValues = StringListImpl.create();
                addedKeywords.forEach(k -> keywordValues.add(k.getKeywordValue()));

                responseBuilder.append("\tKeywords added: ").append(keywordValues.toSeparatedString(", "))
                    .append(System.lineSeparator());
            }

            if (removedKeywords != null && !removedKeywords.isEmpty()) {
                StringList keywordValues = StringListImpl.create();
                removedKeywords.forEach(k -> keywordValues.add(k.getKeywordValue()));

                responseBuilder.append("\tKeywords removed: ").append(keywordValues.toSeparatedString(", "))
                    .append(System.lineSeparator());
            }

            if (changedKeywords != null && !changedKeywords.isEmpty()) {
                List<Keyword> existingKeywords = changedKeywords.stream().map(KeywordChangingEvent::getExistingKeyword).collect(Collectors.toList());
                StringList keywordValues = StringListImpl.create(Keyword.getAllKeywordValues(existingKeywords));

                if (event instanceof UpperCaseKeywordEvent) {
                    responseBuilder.append("\tKeywords set to lower case: ").append(keywordValues.toSeparatedString(", "))
                        .append(System.lineSeparator());
                } else {
                    responseBuilder.append("\tReplace flag adjusted on keywords: ").append(keywordValues.toSeparatedString(", ")).
                        append(System.lineSeparator());
                }
            }
        } else {
            responseBuilder.append("Emoji ").append(event.getSource().getEmojiValue())
                .append(" already exists as is. No changes have been made.");
        }

        alertService.send(responseBuilder.toString(), channel);
    }

}
