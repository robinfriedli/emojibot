package core;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.Context;
import net.robinfriedli.jxp.persist.DefaultPersistenceManager;

/**
 * Contains several tasks to add, delete and modify in memory emojis. Use via the Context#invoke() method.
 * The commit() method then uses the XMLManager to modify the XML file
 */
public class PersistenceManager extends DefaultPersistenceManager {

    /**
     * Merges emojis with the same value
     *
     * @param duplicateEmojis Emojis there are duplicates of
     */
    public void mergeDuplicateEmojis(Set<Emoji> duplicateEmojis) {
        // new emojis replacing duplicates
        for (Emoji duplicateEmoji : duplicateEmojis) {
            Context context = duplicateEmoji.getContext();

            // get all Emoji instances with that value
            List<Emoji> duplicates = getAllEmojis(duplicateEmoji.getEmojiValue(), context);
            // get all Keywords of all duplicates
            List<XmlElement> keywords = Lists.newArrayList(Emoji.getAllKeywords(duplicates));

            // set random to true if any of the duplicates is true since it's the default value
            boolean random = duplicates.stream().anyMatch(Emoji::isRandom);
            if (duplicates.stream().allMatch(e -> e instanceof DiscordEmoji)) {
                duplicates.forEach(XmlElement::delete);
                new DiscordEmoji(
                    keywords,
                    duplicateEmoji.getEmojiValue(),
                    random,
                    ((DiscordEmoji) duplicateEmoji).getName(),
                    ((DiscordEmoji) duplicateEmoji).getGuildId(),
                    ((DiscordEmoji) duplicateEmoji).getGuildName(),
                    context
                ).persist();
            } else if (duplicates.stream().noneMatch(e -> e instanceof DiscordEmoji)) {
                duplicates.forEach(XmlElement::delete);
                new Emoji(keywords, duplicateEmoji.getEmojiValue(), random, context).persist();
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
                Context context = keyword.getContext();

                List<Keyword> duplicates = emoji.getDuplicatesOf(keyword);
                boolean replace = duplicates.stream().allMatch(Keyword::isReplace);
                Keyword replacement = new Keyword(keyword.getKeywordValue(), replace, context);
                emoji.removeKeywords(duplicates);
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
    private List<Emoji> getAllEmojis(String emojiValue, Context context) {
        return context.getInstancesOf(Emoji.class).stream()
            .filter(e -> e.getEmojiValue().equals(emojiValue))
            .collect(Collectors.toList());
    }
}
