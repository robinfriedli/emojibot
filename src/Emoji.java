import java.util.ArrayList;
import java.util.List;

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
        List<Keyword> keywords = new ArrayList<>();
        for (Emoji emoji : emojis) {
            keywords.addAll(emoji.getKeywords());
        }
        return keywords;
    }

    public static List<Emoji> loadFromKeyword(List<Keyword> keywords, List<Emoji> emojis) {
        List<Emoji> selectedEmojis = new ArrayList<>();
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
        List<Emoji> selectedEmojis = new ArrayList<>();
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

}
