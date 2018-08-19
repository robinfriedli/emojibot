package api;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.AbstractXmlElement;
import net.robinfriedli.jxp.persist.Context;
import util.DiscordListener;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Emoji extends AbstractXmlElement {

    public Emoji(List<Keyword> keywords, String emojiValue, boolean random, Context context) {
        this(keywords, emojiValue, random, State.CONCEPTION, "emoji", context);
    }

    public Emoji(List<Keyword> keywords, String emojiValue, boolean random, State state, Context context) {
        this(keywords, emojiValue, random, state, "emoji", context);
    }

    public Emoji(List<Keyword> keywords, Map<String, String> attributeMap, String tagName, Context context) {
        super(tagName, attributeMap, Lists.newArrayList(keywords), context);
    }

    public Emoji(List<Keyword> keywords, Map<String, String> attributeMap, String tagName, State state, Context context) {
        super(tagName, attributeMap, Lists.newArrayList(keywords), state, context);
    }

    public Emoji(List<Keyword> keywords, String emojiValue, boolean random, State state, String tagName, Context context) {
        super(tagName, buildAttributes(emojiValue, random), Lists.newArrayList(keywords), state, context);
    }

    static Map<String, String> buildAttributes(String emojiValue, boolean random) {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("value", emojiValue);
        attributeMap.put("random", Boolean.toString(random));
        return attributeMap;
    }

    @Override
    public String getId() {
        return getEmojiValue();
    }

    public List<Keyword> getKeywords() {
        return getSubElementsWithType(Keyword.class);
    }

    public boolean hasKeyword(Keyword keyword) {
        return getSubElements().contains(keyword);
    }

    public boolean hasKeywordValue(String keyword) {
        List<String> keywords = getKeywords().stream().map(Keyword::getKeywordValue).collect(Collectors.toList());
        return keywords.contains(keyword);
    }

    @Nullable
    public Keyword getKeyword(String value) {
        return getKeyword(value, false);
    }

    public Keyword getKeyword(String value, boolean ignoreCase) {
        if (hasKeywordValue(value)) {
            List<Keyword> foundKeywords = getKeywords().stream()
                .filter(k -> ignoreCase
                    ? k.getKeywordValue().equalsIgnoreCase(value)
                    : k.getKeywordValue().equals(value))
                .collect(Collectors.toList());

            if (foundKeywords.size() == 1) {
                return foundKeywords.get(0);
            } else if (foundKeywords.size() > 1) {
                throw new IllegalStateException("Duplicate keywords on emoji " + value + ". Try " + DiscordListener.COMMAND_CLEAN);
            }
        }

        return null;
    }

    public Keyword requireKeyword(String value) {
        Keyword keyword = getKeyword(value);

        if (keyword == null) {
            throw new IllegalStateException("Keyword value " + value + " not found on emoji "
                + getAttribute("value").getValue());
        }

        return keyword;
    }

    public Keyword requireKeywordIgnoreCase(String value) {
        Keyword keyword = getKeyword(value, true);

        if (keyword == null) {
            throw new IllegalStateException("Keyword value " + value + " not found on emoji "
                + getAttribute("value").getValue());
        }

        return keyword;
    }

    public List<Keyword> getDuplicatesOf(Keyword keyword) {
        return getKeywords().stream().filter(k -> k.getKeywordValue().equals(keyword.getKeywordValue())).collect(Collectors.toList());
    }

    public void addKeyword(Keyword keyword) {
        this.addSubElement(keyword);
    }

    public void removeKeyword(Keyword keyword) {
         removeSubElement(keyword);
    }

    public void removeKeywords(List<Keyword> keywords) {
        removeSubElements(Lists.newArrayList(keywords));
    }

    public void removeKeywords(Keyword... keywords) {
        removeKeywords(Arrays.asList(keywords));
    }

    public String getEmojiValue() {
        return getAttribute("value").getValue();
    }

    public void setEmojiValue(String emoji) {
        setAttribute("value", emoji);
    }

    public boolean isRandom() {
        return Boolean.parseBoolean(getAttribute("random").getValue());
    }

    public void setRandom(boolean random) {
        setAttribute("random", Boolean.toString(random));
    }

    public static List<Keyword> getAllKeywords(List<? extends Emoji> emojis) {
        List<Keyword> keywords = Lists.newArrayList();
        for (Emoji emoji : emojis) {
            keywords.addAll(emoji.getKeywords());
        }
        return keywords;
    }

    public static List<Emoji> loadFromKeyword(List<Keyword> keywords, List<Emoji> emojis) {
        List<Emoji> selectedEmojis = Lists.newArrayList();
        for (Emoji emoji : emojis) {
            List<Keyword> keywordsForEmoji = emoji.getKeywords();
            for (Keyword keyword : keywordsForEmoji) {
                if (keywords.contains(keyword)) {
                    selectedEmojis.add(emoji);
                }
            }
        }
        return selectedEmojis;
    }

    public static List<Emoji> loadFromKeyword(Keyword keyword, List<Emoji> emojis) {
        return emojis.stream()
            .filter(e -> !(e instanceof DiscordEmoji))
            .filter(e -> e.hasKeywordValue(keyword.getKeywordValue()))
            .collect(Collectors.toList());
    }

    public static Emoji loadFromValue(String value, List<Emoji> emojis) {
        List<Emoji> foundEmojis = emojis.stream().filter(e -> e.getEmojiValue().equals(value)).collect(Collectors.toList());

        if (foundEmojis.size() > 1) {
            throw new IllegalStateException("Emoji value: " + value + " not unique, try " + DiscordListener.COMMAND_CLEAN + " or fix your xml file manually");
        } else if (foundEmojis.size() == 1) {
            return foundEmojis.get(0);
        } else {
            throw new IllegalStateException("No emoji found for value: " + value + " within provided list");
        }
    }
/*
    public enum State {

        /**
         * Emoji has been created but not yet persisted
         *//*
        CONCEPTION {
            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                throw new UnsupportedOperationException("Trying to call addChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call getChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public void clearChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call clearChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }
        },

        /**
         * Emoji exists in XML file and was left unchanged
         *//*
        CLEAN {
            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                throw new UnsupportedOperationException("Trying to call addChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call getChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public void clearChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call clearChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }
        },

        /**
         * Emoji exists in XML but has uncommitted changes
         *//*
        TOUCHED {
            private List<EmojiChangingEvent> changes = Lists.newArrayList();

            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                changes.add(emojiChangingEvent);
                Emoji source = emojiChangingEvent.getSource();

                if (emojiChangingEvent instanceof DuplicateKeywordEvent) {
                    context.invoke(false, persistenceManager -> {
                        persistenceManager.applyDuplicateKeywordEvent((DuplicateKeywordEvent) emojiChangingEvent);
                        return null;
                    });
                } else {
                    context.invoke(false, persistenceManager -> {
                            persistenceManager.applyEmojiChanges(source, emojiChangingEvent);
                            return null;
                        }
                    );
                }
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                return this.changes.stream().filter(c -> c.getSource().equals(source)).collect(Collectors.toList());
            }

            @Override
            public void clearChanges(Emoji source) {
                List<EmojiChangingEvent> changesToRemove = Lists.newArrayList();
                if (!changes.isEmpty()) {
                    for (EmojiChangingEvent event : changes) {
                        if (event.getSource().equals(source)) {
                            changesToRemove.add(event);
                        }
                    }
                }
                changes.removeAll(changesToRemove);
            }
        },

        /**
         * Emoji is being deleted but still exists in XML file
         *//*
        DELETION {
            @Override
            public void addChanges(EmojiChangingEvent emojiChangingEvent, Context context) {
                throw new UnsupportedOperationException("Trying to call addChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public List<EmojiChangingEvent> getChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call getChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }

            @Override
            public void clearChanges(Emoji source) {
                throw new UnsupportedOperationException("Trying to call clearChanges() on an Emoji that is not in state TOUCHED but " + this.toString());
            }
        };

        public abstract void addChanges(EmojiChangingEvent emojiChangingEvent, Context context);

        public abstract List<EmojiChangingEvent> getChanges(Emoji source);

        public abstract void clearChanges(Emoji source);

    }
**/
}
