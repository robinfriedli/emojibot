package core;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;
import org.w3c.dom.Element;
import util.DiscordListener;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains several tasks to add, delete and modify in memory emojis. Use via the Context#invoke() method.
 * The commit() method then uses the XMLManager to modify the XML file
 */
public class PersistenceManager extends DefaultPersistenceManager {

    @Override
    public List<XmlElement> getAllElements() {
        Iterable<Emoji> concatList = Iterables.concat(getUnicodeEmojis(), getDiscordEmojis());
        return Lists.newArrayList(concatList);
    }

    /**
     * Merges emojis with the same value
     *
     * @param duplicateEmojis Emojis there are duplicates of
     */
    public void mergeDuplicateEmojis(Set<Emoji> duplicateEmojis) {
        // new emojis replacing duplicates
        for (Emoji duplicateEmoji : duplicateEmojis) {
            // get all Emoji instances with that value
            List<Emoji> duplicates = getAllEmojis(duplicateEmoji.getEmojiValue());
            // get all Keywords of all duplicates
            List<Keyword> keywords = Emoji.getAllKeywords(duplicates);

            // set random to true if any of the duplicates is true since it's the default value
            boolean random = duplicates.stream().anyMatch(Emoji::isRandom);
            if (duplicates.stream().allMatch(e -> e instanceof DiscordEmoji)) {
                duplicates.forEach(e -> e.setState(XmlElement.State.DELETION));
                getContext().removeElements(Lists.newArrayList(duplicates));
                List<Element> duplicateElems = getXmlPersister().find("discord-emoji", "value", duplicateEmoji.getEmojiValue());
                duplicateElems.forEach(elem -> elem.getParentNode().removeChild(elem));
                new DiscordEmoji(
                        keywords,
                        duplicateEmoji.getEmojiValue(),
                        random,
                        ((DiscordEmoji) duplicateEmoji).getName(),
                        ((DiscordEmoji) duplicateEmoji).getGuildId(),
                        ((DiscordEmoji) duplicateEmoji).getGuildName(),
                        getContext()
                ).persist();
            } else if (duplicates.stream().noneMatch(e -> e instanceof DiscordEmoji)) {
                duplicates.forEach(e -> e.setState(XmlElement.State.DELETION));
                getContext().removeElements(Lists.newArrayList(duplicates));
                List<Element> duplicateElems = getXmlPersister().find("emoji", "value", duplicateEmoji.getEmojiValue());
                duplicateElems.forEach(elem -> elem.getParentNode().removeChild(elem));
                new Emoji(keywords, duplicateEmoji.getEmojiValue(), random, getContext()).persist();
            } else {
                throw new IllegalStateException("Not all duplicates of " + duplicateEmoji.getEmojiValue()
                        + " are of the same type. Merging failed.");
            }
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
            // loop over the different keywords there are duplicates of
            for (Keyword keyword : duplicateKeywords.get(emoji)) {
                List<Keyword> duplicates = emoji.getDuplicatesOf(keyword);
                boolean replace = duplicates.stream().allMatch(Keyword::isReplace);
                Keyword replacement = new Keyword(keyword.getKeywordValue(), replace, getContext());
                getContext().apply(() -> emoji.removeKeywords(duplicates));
                List<Element> duplicateElements = getXmlPersister().find("keyword", keyword.getTextContent(), emoji);
                duplicateElements.forEach(elem -> elem.getParentNode().removeChild(elem));
                emoji.addKeyword(replacement);
            }
        }

    }

    public void handleUpperCaseKeywords(Multimap<Emoji, Keyword> upperCaseKeywords) {
        for (Emoji emoji : upperCaseKeywords.keySet()) {
            Collection<Keyword> keywords = upperCaseKeywords.get(emoji);

            for (Keyword keyword : keywords) {
                keyword.setTextContent(keyword.getTextContent().toLowerCase());
            }

        }
    }

    /**
     * used to load duplicate Emojis
     *
     * @param emojiValue value of the emojis
     * @return all Emoji instances with specified value
     */
    private List<Emoji> getAllEmojis(String emojiValue) {
        return getContext().getInstancesOf(Emoji.class).stream()
                .filter(e -> e.getEmojiValue().equals(emojiValue))
                .collect(Collectors.toList());
    }

    public List<Emoji> getUnicodeEmojis() {
        List<Emoji> emojis = Lists.newArrayList();
        List<Element> emojiElems = getXmlPersister().getElements("emoji");

        for (Element emojiElem : emojiElems) {
            String randomValue = emojiElem.getAttribute("random");
            String emojiValue = emojiElem.getAttribute("value");
            boolean random = randomValue.equals("") || Boolean.parseBoolean(randomValue);
            emojis.add(new Emoji(getKeywords(emojiElem), emojiValue, random, Emoji.State.CLEAN, getContext()));
        }

        return emojis;
    }

    public List<DiscordEmoji> getDiscordEmojis() {
        List<DiscordEmoji> discordEmojis = Lists.newArrayList();
        List<Element> emojiElems = getXmlPersister().getElements("discord-emoji");

        for (Element emojiElem : emojiElems) {
            String value = emojiElem.getAttribute("value");
            String randomValue = emojiElem.getAttribute("random");
            String name = emojiElem.getAttribute("name");
            String guildId = emojiElem.getAttribute("guildId");
            String guildName = emojiElem.getAttribute("guildName");
            boolean random = randomValue.equals("") || Boolean.parseBoolean(randomValue);

            discordEmojis.add(new DiscordEmoji(getKeywords(emojiElem), value, random, Emoji.State.CLEAN, name, guildId, guildName, getContext()));
        }

        return discordEmojis;
    }

    private List<Keyword> getKeywords(Element emojiElem) {
        List<Keyword> keywords = Lists.newArrayList();
        List<Element> keywordElems = getXmlPersister().getChildren(emojiElem);

        for (Element keywordElem : keywordElems) {
            boolean replace = Boolean.parseBoolean(keywordElem.getAttribute("replace"));
            String keywordValue = keywordElem.getTextContent();
            keywords.add(new Keyword(keywordValue, replace, XmlElement.State.CLEAN, getContext()));
        }

        return keywords;
    }

    @Nullable
    private <E extends Emoji> E getEmoji(String value) {
        List<? extends Emoji> foundEmojis = getContext().getInstancesOf(Emoji.class).stream()
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
