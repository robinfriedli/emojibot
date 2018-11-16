package util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import net.robinfriedli.jxp.events.EventListener;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.Transaction;
import net.robinfriedli.stringlist.StringList;
import net.robinfriedli.stringlist.StringListImpl;

public class AlertEventListener extends EventListener {

    private final AlertService alertService;

    public AlertEventListener(AlertService alertService) {
        this.alertService = alertService;
    }

    @Override
    public void transactionApplied(Transaction tx) {
        Context context = tx.getContext();
        List<Emoji> createdEmojis = Lists.newArrayList();
        List<Emoji> deletedEmojis = Lists.newArrayList();
        Multimap<Emoji, Keyword> createdKeywords = HashMultimap.create();
        Multimap<Emoji, Keyword> deletedKeywords = HashMultimap.create();
        Multimap<Emoji, ElementChangingEvent> changedEmojis = HashMultimap.create();
        List<ElementChangingEvent> changedKeywords = Lists.newArrayList();

        for (ElementCreatedEvent createdElement : tx.getCreatedElements()) {
            XmlElement source = createdElement.getSource();
            if (source instanceof Emoji) {
                createdEmojis.add((Emoji) source);
            } else if (source instanceof Keyword) {
                Emoji emoji = (Emoji) source.getParent();
                createdKeywords.put(emoji, (Keyword) source);
            }
        }

        for (ElementDeletingEvent deletedElement : tx.getDeletedElements()) {
            XmlElement source = deletedElement.getSource();
            if (source instanceof Emoji) {
                deletedEmojis.add((Emoji) source);
            } else if (source instanceof Keyword) {
                Emoji emoji = (Emoji) deletedElement.getOldParent();
                if (emoji.getState().isPhysical()) {
                    deletedKeywords.put(emoji, (Keyword) source);
                }
            }
        }

        for (ElementChangingEvent change : tx.getElementChanges()) {
            if (change.getSource() instanceof Emoji) {
                changedEmojis.put((Emoji) change.getSource(), change);
            } else if (change.getSource() instanceof Keyword) {
                changedKeywords.add(change);
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
        if (!changedEmojis.isEmpty() || !createdKeywords.isEmpty() || !deletedKeywords.isEmpty()) {
            alertEmojiChanges(responseBuilder, changedEmojis, createdKeywords, deletedKeywords);
        }
        if (!changedKeywords.isEmpty()) {
            for (ElementChangingEvent changedKeyword : changedKeywords) {
                Keyword keyword = (Keyword) changedKeyword.getSource();
                if (changedKeyword.attributeChanged("replace")) {
                    String replace = changedKeyword.getAttributeChange("replace").getNewValue();
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

    private void alertEmojiChanges(StringBuilder responseBuilder,
                                   Multimap<Emoji, ElementChangingEvent> changedEmojis,
                                   Multimap<Emoji, Keyword> createdKeywordsMap,
                                   Multimap<Emoji, Keyword> deletedKeywordsMap) {
        Set<Emoji> affectedEmojis = Stream
                .concat(
                        changedEmojis.keySet().stream(),
                        Stream.concat(
                                createdKeywordsMap.keySet().stream(), deletedKeywordsMap.keySet().stream()
                        )
                )
                .collect(Collectors.toSet());
        for (Emoji emoji : affectedEmojis) {
            responseBuilder.append("Emoji changed: ")
                    .append(emoji.getEmojiValue()).append(System.lineSeparator());
            for (ElementChangingEvent changedEmoji : changedEmojis.get(emoji)) {
                if (changedEmoji.attributeChanged("random")) {
                    responseBuilder.append("\trandom flag changed to ")
                            .append(changedEmoji.getAttributeChange("random").getNewValue())
                            .append(System.lineSeparator());
                }
            }

            Collection<Keyword> createdKeywords = createdKeywordsMap.get(emoji);
            if (!createdKeywords.isEmpty()) {
                String keywordString = StringListImpl.create(createdKeywords, Keyword::getKeywordValue).toSeparatedString(", ");
                responseBuilder.append("\tKeywords added: ").append(keywordString).append(System.lineSeparator());
            }

            Collection<Keyword> deletedKeywords = deletedKeywordsMap.get(emoji);
            if (!deletedKeywords.isEmpty()) {
                String keywordString = StringListImpl.create(deletedKeywords, Keyword::getKeywordValue).toSeparatedString(", ");
                responseBuilder.append("\tKeywords removed: ").append(keywordString).append(System.lineSeparator());
            }
        }
    }

}
