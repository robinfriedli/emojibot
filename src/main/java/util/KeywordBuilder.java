package util;

import api.Keyword;
import net.robinfriedli.jxp.persist.Context;

public class KeywordBuilder {

    private String keywordValue;
    private Boolean replace;

    public String getKeywordValue() {
        return keywordValue;
    }

    public KeywordBuilder setKeywordValue(String keywordValue) {
        this.keywordValue = keywordValue;
        return this;
    }

    public KeywordBuilder setReplace(boolean replace) {
        this.replace = replace;
        return this;
    }

    public boolean isReplace() {
        return replace;
    }

    public Keyword createKeyword(Context context) {
        assert keywordValue != null;
        assert replace != null;

        return new Keyword(keywordValue, replace, context);
    }
}
