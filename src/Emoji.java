import com.google.common.collect.Lists;

import java.util.List;
import java.util.stream.Collectors;

public class Emoji {

    private List<Keyword> keywords;
    private String emojiValue;

    public Emoji(List<Keyword> keywords, String emojiValue) {
        this.keywords = keywords;
        this.emojiValue = emojiValue;
    }

    public List<Keyword> getKeywords() {
        return keywords;
    }

    public Keyword getKeyword(String value) {
        for (Keyword keyword : keywords) {
            if (keyword.getKeywordValue().equals(value)) {
                return keyword;
            }
        }

        return null;
    }

    public void setKeywords(List<Keyword> keywords) {
        this.keywords = keywords;
    }

    public String getEmojiValue() {
        return emojiValue;
    }

    public void setEmojiValue(String emoji) {
        this.emojiValue = emoji;
    }

    public static List<Keyword> getAllKeywords(List<Emoji> emojis) {
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
        List<Emoji> selectedEmojis = Lists.newArrayList();
        for (Emoji emoji : emojis) {
            List<Keyword> keywordsForEmoji = emoji.getKeywords();
            for (Keyword keywordForEmoji : keywordsForEmoji) {
                if (keywordForEmoji.getKeywordValue().equals(keyword.getKeywordValue())) {
                    selectedEmojis.add(emoji);
                }
            }
        }
        return selectedEmojis;
    }

    public static Emoji loadFromValue(String value, List<Emoji> emojis) {
        List<Emoji> foundEmojis = emojis.stream().filter(e -> e.getEmojiValue().equals(value)).collect(Collectors.toList());

        if (foundEmojis.size() > 1) {
            throw new IllegalStateException("emoji value: " + value + " not unique, use the clean command or fix your xml file manually");
        } else if (foundEmojis.size() == 1) {
            return foundEmojis.get(0);
        }

        return null;
    }

}
