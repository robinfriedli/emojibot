package util;

import api.Emoji;
import api.Keyword;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.EventListener;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.stringlist.StringList;
import net.robinfriedli.stringlist.StringListImpl;

import java.util.List;

public class AlertEventListener extends EventListener {

    private final AlertService alertService;

    public AlertEventListener(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void elementCreating(ElementCreatedEvent event) {
        MessageChannel channel = null;
        Context context = event.getSource().getContext();
        if (context.getEnvVar() instanceof MessageChannel) {
            channel = (MessageChannel) context.getEnvVar();
        }

        Emoji emoji = (Emoji) event.getSource();
        alertService.send("Emoji added: " + emoji.getEmojiValue(), channel);
    }

    @Override
    public void elementDeleting(ElementDeletingEvent event) {
        MessageChannel channel = null;
        Context context = event.getSource().getContext();
        if (context.getEnvVar() instanceof MessageChannel) {
            channel = (MessageChannel) context.getEnvVar();
        }

        Emoji emoji = (Emoji) event.getSource();
        alertService.send("Emoji deleted: " + emoji.getEmojiValue(), channel);
    }

    @Override
    public void elementChanging(ElementChangingEvent event) {
        MessageChannel channel = null;
        Context context = event.getSource().getContext();
        if (context.getEnvVar() instanceof MessageChannel) {
            channel = (MessageChannel) context.getEnvVar();
        }

        StringBuilder responseBuilder = new StringBuilder();
        if (event.getSource() instanceof Emoji) {
            Emoji emoji = (Emoji) event.getSource();

            List<XmlElement> addedKeywords = event.getAddedSubElements();
            List<XmlElement> removedKeywords = event.getRemovedSubElements();

            if (event.attributeChanged("random")
                || (addedKeywords != null && !addedKeywords.isEmpty())
                || (removedKeywords != null && !removedKeywords.isEmpty())) {

                responseBuilder.append("Emoji changed: ").append(emoji.getEmojiValue()).append(System.lineSeparator());

                if (event.attributeChanged("random")) {
                    String randomTag = event.getAttributeChange("random").getNewValue().getValue();
                    responseBuilder.append("\tRandom flag changed to ").append(randomTag).append(System.lineSeparator());
                }

                if (addedKeywords != null && !addedKeywords.isEmpty()) {
                    StringList keywordValues = StringListImpl.create();
                    addedKeywords.forEach(k -> keywordValues.add(((Keyword) k).getKeywordValue()));

                    responseBuilder.append("\tKeywords added: ").append(keywordValues.toSeparatedString(", "))
                        .append(System.lineSeparator());
                }

                if (removedKeywords != null && !removedKeywords.isEmpty()) {
                    StringList keywordValues = StringListImpl.create();
                    removedKeywords.forEach(k -> keywordValues.add(((Keyword) k).getKeywordValue()));

                    responseBuilder.append("\tKeywords removed: ").append(keywordValues.toSeparatedString(", "))
                        .append(System.lineSeparator());
                }
            }
        } else if (event.getSource() instanceof  Keyword) {
            Keyword keyword = (Keyword) event.getSource();

            if (event.attributeChanged("replace")) {
                responseBuilder.append("Replace value adjusted on keyword ").append(keyword.getKeywordValue())
                    .append(System.lineSeparator());
            }
        }

        if (responseBuilder.toString().equals("")) {
            if (event.getSource() instanceof Emoji) {
                Emoji emoji = (Emoji) event.getSource();
                responseBuilder.append("Emoji ").append(emoji.getEmojiValue()).append(" already exists as is. ")
                    .append(System.lineSeparator());
            }
            responseBuilder.append("No changes have been made").append(System.lineSeparator());
        }
        alertService.send(responseBuilder.toString(), channel);
    }

}
