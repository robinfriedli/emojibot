import com.google.common.collect.Lists;

import java.util.List;

public class Keyword {

    private String keywordValue;
    private boolean replace;

    public Keyword(String keywordValue, boolean replace) {
        this.keywordValue = keywordValue;
        this.replace = replace;
    }

    public String getKeywordValue() {
        return keywordValue;
    }

    public void setKeywordValue(String keyword) {
        this.keywordValue = keyword;
    }

    public boolean isReplace() {
        return replace;
    }

    public void setReplace(boolean replace) {
        this.replace = replace;
    }

    public static List<String> getAllKeywordValues(List<Keyword> keywords) {
        List<String> keywordValues = Lists.newArrayList();
        for (Keyword keyword : keywords) {
            keywordValues.add(keyword.getKeywordValue());
        }
        return keywordValues;
    }

    public static List<Keyword> getSelectedKeywords(List<Keyword> keywords, String value) {
        List<Keyword> selectedKeywords = Lists.newArrayList();
        for (Keyword keyword : keywords) {
            if (keyword.getKeywordValue().equals(value)) {
                selectedKeywords.add(keyword);
            }
        }
        return selectedKeywords;
    }

}
