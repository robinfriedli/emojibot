package util;

import java.util.List;

import api.Emoji;
import api.Keyword;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.events.ElementChangingEvent;
import net.robinfriedli.jxp.events.ElementCreatedEvent;
import net.robinfriedli.jxp.events.ElementDeletingEvent;
import net.robinfriedli.jxp.events.Event;
import net.robinfriedli.jxp.events.EventListener;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.stringlist.StringList;
import net.robinfriedli.stringlist.StringListImpl;

public class AlertEventListener extends EventListener {

    private final AlertService alertService;

    public AlertEventListener(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void transactionApplied(List<Event> events, Context context) {
        List<Emoji> createdEmojis = Lists.newArrayList();
        List<Emoji> deletedEmojis = Lists.newArrayList();
        Multimap<Emoji, ElementChangingEvent> changedEmojis = HashMultimap.create();
        List<ElementChangingEvent> changedKeywords = Lists.newArrayList();

        for (Event event : events) {
            if (event instanceof ElementCreatedEvent) {
                createdEmojis.add((Emoji) event.getSource());
            } else if (event instanceof ElementDeletingEvent) {
                deletedEmojis.add((Emoji) event.getSource());
            } else if (event instanceof ElementChangingEvent) {
                if (event.getSource() instanceof Emoji) {
                    changedEmojis.put((Emoji) event.getSource(), (ElementChangingEvent) event);
                } else if (event.getSource() instanceof Keyword) {
                    changedKeywords.add((ElementChangingEvent) event);
                }
            }
        }

        StringBuilder responseBuilder = new StringBuilder();
        if (!createdEmojis.isEmpty()) {
            StringList emojiList = StringListImpl.create();
            createdEmojis.forEach(e -> emojiList.add(e.getEmojiValue()));
            responseBuilder.append("Emojis created: ").append(emojiList.toSeparatedString(" ")).append(System.lineSeparator());
        }
        if (!deletedEmojis.isEmpty()) {
            StringList emojiList = StringListImpl.create();
            deletedEmojis.forEach(e -> emojiList.add(e.getEmojiValue()));
            responseBuilder.append("Emojis deleted: ").append(emojiList.toSeparatedString(" ")).append(System.lineSeparator());
        }
        if (!changedEmojis.isEmpty()) {
            alertEmojiChanges(responseBuilder, changedEmojis);
        }
        if (!changedKeywords.isEmpty()) {
            for (ElementChangingEvent changedKeyword : changedKeywords) {
                Keyword keyword = (Keyword) changedKeyword.getSource();
                if (changedKeyword.attributeChanged("replace")) {
                    String replace = changedKeyword.getAttributeChange("replace").getNewValue().getValue();
                    String emojiValue = ((Emoji) keyword.getParent()).getEmojiValue();
                    responseBuilder.append("Replace set to ").append(replace)
                        .append(" for Keyword ").append(keyword.getKeywordValue()).append(" on Emoji ").append(emojiValue)
                        .append(System.lineSeparator());
                }
            }
        }

        String response = responseBuilder.toString();
        MessageChannel channel = null;
        if (context.getEnvVar() instanceof MessageChannel) {
            channel = (MessageChannel) context.getEnvVar();
        }
        if (!"".equals(response)) {
            alertService.send(response, channel);
        } else {
            alertService.send("No changes have been made", channel);
        }
    }

    private void alertEmojiChanges(StringBuilder responseBuilder, Multimap<Emoji, ElementChangingEvent> changedEmojis) {
        for (Emoji emoji : changedEmojis.keySet()) {
            responseBuilder.append("Emoji changed: ")
                .append(emoji.getEmojiValue()).append(System.lineSeparator());
            for (ElementChangingEvent changedEmoji : changedEmojis.get(emoji)) {
                if (changedEmoji.attributeChanged("random")) {
                    responseBuilder.append("\trandom flag changed to ")
                        .append(changedEmoji.getAttributeChange("random").getNewValue().getValue())
                        .append(System.lineSeparator());
                }

                List<XmlElement> addedSubElements = changedEmoji.getAddedSubElements();
                if (addedSubElements != null && !addedSubElements.isEmpty()) {
                    StringList addedKeywords = StringListImpl.create();
                    addedSubElements.forEach(k -> addedKeywords.add(((Keyword) k).getKeywordValue()));
                    responseBuilder.append("\tKeywords added: ").append(addedKeywords.toSeparatedString(" "))
                        .append(System.lineSeparator());
                }

                List<XmlElement> removedSubElements = changedEmoji.getRemovedSubElements();
                if (removedSubElements != null && !removedSubElements.isEmpty()) {
                    StringList removedKeywords = StringListImpl.create();
                    removedSubElements.forEach(k -> removedKeywords.add(((Keyword) k).getKeywordValue()));
                    responseBuilder.append("\tKeywords removed: ").append(removedKeywords.toSeparatedString(" "))
                        .append(System.lineSeparator());
                }
            }
        }
    }

}
