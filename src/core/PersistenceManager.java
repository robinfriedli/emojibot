package core;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.w3c.dom.Element;
import util.AlertService;
import util.DiscordListener;

/**
 * Contains several tasks to add, delete and modify in memory emojis. Use via the Context#executePersistTask() method.
 * The commit() method then uses the XMLManager to modify the XML file
 */
public class PersistenceManager {

    private final Context context;
    private final XmlManager xmlManager;

    public PersistenceManager(Context context, XmlManager xmlManager) {
        this.context = context;
        this.xmlManager = xmlManager;
    }

    /**
     * persists all in memory changes to the XML file
     */
    public void commit() {
        for (Emoji emoji : context.getInMemoryEmojis()) {
            switch (emoji.getState()) {
                case CONCEPTION:
                    xmlManager.addEmojiElem(emoji);
                    break;
                case DELETION:
                    xmlManager.removeEmojiElem(emoji);
                    break;
                case TOUCHED:
                    commitEmojiChanges(emoji);
                    emoji.getState().clearChanges(emoji);
            }
        }
        xmlManager.writeToFile();
        context.reloadEmojis();
    }

    /**
     * adds new Emoji to memory or loads the existing one with the same value and creates an EmojiChangingEvent for it
     *
     * @param emojis to add
     * @param <E> Emoji or a subclass e.g. DiscordEmoji
     */
    public <E extends Emoji> void addEmojis(E... emojis) {
        for (E emoji : emojis) {
            Emoji existingEmoji = getEmoji(emoji.getEmojiValue());
            if (existingEmoji == null) {
                context.addEmojiToMemory(emoji);
                context.fireEmojiCreating(new Event(emoji));
            } else {
                existingEmoji.setState(Emoji.State.TOUCHED);
                Boolean random = emoji.isRandom() == existingEmoji.isRandom() ? null : emoji.isRandom();

                List<Keyword> addedKeywords = Lists.newArrayList();
                List<KeywordChangingEvent> changedKeywords = Lists.newArrayList();

                for (Keyword keyword : emoji.getKeywords()) {
                    Keyword existingKeyword = existingEmoji.getKeyword(keyword.getKeywordValue());
                    if (existingKeyword == null) {
                        addedKeywords.add(keyword);
                    } else {
                        if (keyword.isReplace() != existingKeyword.isReplace()) {
                            changedKeywords.add(new KeywordChangingEvent(emoji, existingKeyword, keyword));
                        }
                    }
                }

                existingEmoji.getState()
                    .addChanges(new EmojiChangingEvent(random, addedKeywords, null, changedKeywords, existingEmoji), context);
            }
        }
    }

    /**
     * Sets Emojis to state DELETION. These Emojis will be ignored but not removed from XML file.
     * These Emojis will return when the Emojis are reloaded before the changes were committed.
     *
     * @param emojiValues to delete
     */
    public void deleteEmojis(List<String> emojiValues) {
        for (String emoji : emojiValues) {
            try {
                Emoji foundEmoji = requireEmoji(emoji);
                foundEmoji.setState(Emoji.State.DELETION);
                context.fireEmojiDeleting(new Event(foundEmoji));
            } catch (IllegalStateException e) {
                AlertService alertService = new AlertService();
                alertService.send(e.getMessage(), context.getChannel());
            }
        }
    }

    /**
     * removes keywords with specified value from in memory Emoji with specified value
     *
     * @param emojiValue value of Emoji in memory
     * @param keywordValues values of Keywords to remove
     */
    public void deleteKeywords(String emojiValue, String... keywordValues) {
        List<Keyword> keywordsToDelete = Lists.newArrayList();
        Emoji emoji = Emoji.loadFromValue(emojiValue, context.getInMemoryEmojis());

        for (String keywordValue : keywordValues) {
            try {
                keywordsToDelete.add(emoji.requireKeyword(keywordValue));
            } catch (IllegalStateException e) {
                AlertService alertService = new AlertService();
                alertService.send(e.getMessage(), context.getChannel());
            }
        }

        emoji.setState(Emoji.State.TOUCHED);
        emoji.getState()
            .addChanges(new EmojiChangingEvent(null, null, keywordsToDelete, null, emoji), context);
    }

    /**
     * Applies EmojiChangingEvent to Emoji in state TOUCHED. This method gets called each time Emoji.State#addChanges is called.
     * The state keeps a List of all uncommitted EmojiChangingEvent instances.
     * Changes are applied to in memory Emojis and only persisted when commit() is called
     *
     * @param emoji to apply the changes on
     * @param changes EmojiChangingEvent instance
     * @param <E> Emoji or a subclass e.g. DiscordEmoji
     */
    public <E extends Emoji> void applyEmojiChanges(E emoji, EmojiChangingEvent changes) {
        applyEmojiChanges(emoji, changes, false);
        context.fireEmojiChanging(changes);
    }

    /**
     * Merges emojis with the same value
     *
     * @param duplicateEmojis Emojis there are duplicates of
     */
    public void mergeDuplicateEmojis(Set<Emoji> duplicateEmojis) {
        // new emojis replacing duplicates
        List<Emoji> newEmojis = Lists.newArrayList();
        for (Emoji duplicateEmoji : duplicateEmojis) {
            // get all Emoji instances with that value
            List<Emoji> duplicates = getAllEmojis(duplicateEmoji.getEmojiValue());
            // get all Keywords of all duplicates
            List<Keyword> keywords = Emoji.getAllKeywords(duplicates);

            // set random to true if any of the duplicates is true since it's the default value
            boolean random = duplicates.stream().anyMatch(Emoji::isRandom);
            if (duplicates.stream().allMatch(e -> e instanceof DiscordEmoji)) {
                newEmojis.add(new DiscordEmoji(
                    keywords,
                    duplicateEmoji.getEmojiValue(),
                    random,
                    ((DiscordEmoji) duplicateEmoji).getName(),
                    ((DiscordEmoji) duplicateEmoji).getGuildId(),
                    ((DiscordEmoji) duplicateEmoji).getGuildName()
                ));

                duplicates.forEach(e -> e.setState(Emoji.State.DELETION));
            } else if (duplicates.stream().noneMatch(e -> e instanceof DiscordEmoji)) {
                newEmojis.add(new Emoji(keywords, duplicateEmoji.getEmojiValue(), random));
                duplicates.forEach(e -> e.setState(Emoji.State.DELETION));
            } else {
                throw new IllegalStateException("Not all duplicates of " + duplicateEmoji.getEmojiValue()
                    + " are of the same type. Merging failed.");
            }
        }

        for (Emoji newEmoji : newEmojis) {
            context.addEmojiToMemory(newEmoji);
        }
    }

    /**
     * merges duplicate Keywords on the same Emoji
     *
     * @param duplicateKeywords Map with Keywords there are duplicates of and their emoji as key
     */
    public void mergeDuplicateKeywords(Multimap<Emoji, Keyword> duplicateKeywords) {
        // loop over all emojis that have duplicate keywords
        for (Emoji emoji : duplicateKeywords.keySet()) {
            emoji.setState(Emoji.State.TOUCHED);
            // loop over the different keywords there are duplicates of
            for (Keyword keyword : duplicateKeywords.get(emoji)) {
                List<Keyword> duplicates = emoji.getDuplicatesOf(keyword);
                boolean replace = duplicates.stream().allMatch(Keyword::isReplace);
                Keyword replacement = new Keyword(keyword.getKeywordValue(), replace);
                emoji.getState().addChanges(new DuplicateKeywordEvent(replacement, duplicates, emoji), context);
            }
        }
    }

    public void applyDuplicateKeywordEvent(DuplicateKeywordEvent event) {
        Emoji emoji = event.getSource();
        event.getDuplicates().forEach(emoji::removeAll);
        emoji.addKeyword(event.getReplacement());

        context.fireEmojiChanging(event);
    }

    public void handleUpperCaseKeywords(Multimap<Emoji, Keyword> upperCaseKeywords) {
        for (Emoji emoji : upperCaseKeywords.keys()) {
            emoji.setState(Emoji.State.TOUCHED);
            List<KeywordChangingEvent> keywordChanges = Lists.newArrayList();
            Collection<Keyword> keywords = upperCaseKeywords.get(emoji);

            for (Keyword keyword : keywords) {
                keywordChanges.add(new KeywordChangingEvent(
                    emoji,
                    keyword,
                    new Keyword(keyword.getKeywordValue().toLowerCase(), keyword.isReplace())));
            }

            emoji.getState().addChanges(new UpperCaseKeywordEvent(Lists.newArrayList(keywordChanges), emoji), context);
        }
    }

    /**
     * used to load duplicate Emojis
     *
     * @param emojiValue value of the emojis
     * @return all Emoji instances with specified value
     */
    private List<Emoji> getAllEmojis(String emojiValue) {
        return context.getInMemoryEmojis().stream()
            .filter(e -> e.getEmojiValue().equals(emojiValue))
            .collect(Collectors.toList());
    }

    public List<Emoji> getAllEmojis() {
        Iterable<Emoji> concatList = Iterables.concat(getUnicodeEmojis(), getDiscordEmojis());
        return Lists.newArrayList(concatList);
    }

    public List<Emoji> getUnicodeEmojis() {
        List<Emoji> emojis = Lists.newArrayList();
        List<Element> emojiElems = xmlManager.getEmojiElems();

        for (Element emojiElem : emojiElems) {
            String randomValue = emojiElem.getAttribute("random");
            String emojiValue = emojiElem.getAttribute("value");
            boolean random = randomValue.equals("") || Boolean.parseBoolean(randomValue);
            emojis.add(new Emoji(getKeywords(emojiElem), emojiValue, random, Emoji.State.CLEAN));
        }

        return emojis;
    }

    public List<DiscordEmoji> getDiscordEmojis() {
        List<DiscordEmoji> discordEmojis = Lists.newArrayList();
        List<Element> emojiElems = xmlManager.getDiscordEmojiElems();

        for (Element emojiElem : emojiElems) {
            String value = emojiElem.getAttribute("value");
            String randomValue = emojiElem.getAttribute("random");
            String name = emojiElem.getAttribute("name");
            String guildId = emojiElem.getAttribute("guildId");
            String guildName = emojiElem.getAttribute("guildName");
            boolean random = randomValue.equals("") || Boolean.parseBoolean(randomValue);

            discordEmojis.add(new DiscordEmoji(getKeywords(emojiElem), value, random, Emoji.State.CLEAN, name, guildId, guildName));
        }

        return discordEmojis;
    }

    private <E extends Emoji> void applyEmojiChanges(E emoji, EmojiChangingEvent changes, boolean isCommit) {
        Boolean randomTag = changes.getRandomTag();
        if (randomTag != null) {
            if (isCommit) {
                xmlManager.adjustEmojiElem(emoji, randomTag);
            } else {
                emoji.setRandom(randomTag);
            }
        }

        // remove before adding in case it is a DuplicateKeywordEvent (meaning the added and removed keywords have the same value)
        List<Keyword> removedKeywords = changes.getRemovedKeywords();
        if (removedKeywords != null && !removedKeywords.isEmpty()) {
            if (isCommit) {
                if (changes instanceof DuplicateKeywordEvent) {
                    xmlManager.removeAllKeywords(emoji, removedKeywords);
                } else {
                    xmlManager.removeKeywords(emoji, removedKeywords);
                }
            } else {
                removedKeywords.forEach(emoji::removeKeyword);
            }
        }

        List<Keyword> addedKeywords = changes.getAddedKeywords();
        if (addedKeywords != null && !addedKeywords.isEmpty()) {
            if (isCommit) {
                xmlManager.addKeywords(emoji, addedKeywords);
            } else {
                addedKeywords.forEach(emoji::addKeyword);
            }
        }

        List<KeywordChangingEvent> changedKeywords = changes.getChangedKeywords();
        if (changedKeywords != null && !changedKeywords.isEmpty()) {
            if (isCommit) {
                xmlManager.adjustKeywords(emoji, changedKeywords);
            } else {
                for (KeywordChangingEvent changedKeyword : changedKeywords) {
                    Keyword existingKeyword = changedKeyword.getExistingKeyword();
                    Keyword adjustedKeyword = changedKeyword.getAdjustedKeyword();

                    if (existingKeyword.isReplace() != adjustedKeyword.isReplace()) {
                        existingKeyword.setReplace(adjustedKeyword.isReplace());
                    }
                    if (!existingKeyword.getKeywordValue().equals(adjustedKeyword.getKeywordValue())) {
                        existingKeyword.setKeywordValue(adjustedKeyword.getKeywordValue());
                    }
                }
            }
        }
    }

    private <E extends Emoji> void commitEmojiChanges(E emoji) {
        for (EmojiChangingEvent change : emoji.getState().getChanges(emoji)) {
            applyEmojiChanges(emoji, change, true);
        }
    }

    private List<Keyword> getKeywords(Element emojiElem) {
        List<Keyword> keywords = Lists.newArrayList();
        List<Element> keywordElems = xmlManager.getKeywordElems(emojiElem);

        for (Element keywordElem : keywordElems) {
            boolean replace = Boolean.parseBoolean(keywordElem.getAttribute("replace"));
            String keywordValue = keywordElem.getTextContent();
            keywords.add(new Keyword(keywordValue, replace));
        }

        return keywords;
    }

    @Nullable
    private <E extends Emoji> E getEmoji(String value) {
        List<? extends Emoji> foundEmojis = context.getInMemoryEmojis().stream()
            .filter(e -> e.getEmojiValue().equals(value)).collect(Collectors.toList());

        if (foundEmojis.size() == 1) {
            return (E) foundEmojis.get(0);
        } else if (foundEmojis.isEmpty()) {
            return null;
        } else {
            throw new IllegalStateException("Duplicate emojis. Try " + DiscordListener.COMMAND_CLEAN);
        }
    }

    private <E extends Emoji> E requireEmoji(String value) {
        E emoji = getEmoji(value);
        if (emoji != null) {
            return emoji;
        } else {
            throw new IllegalStateException("No emoji found for value " + value);
        }
    }
}
