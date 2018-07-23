package core;

import api.Emoji;
import api.Keyword;

public class KeywordChangingEvent extends Event {

    // keyword that already exists in memory you wish to change
    private final Keyword existingKeyword;
    // new keyword that holds the adjustments you wish to apply to the existing one
    private final Keyword adjustedKeyword;
    // save initial value of existingKeyword for commit so that the XmlManager can find the Element
    // (since commit() gets called after changes are already applied to the in memory Emoji instance)
    private final String oldValue;

    public KeywordChangingEvent(Emoji source, Keyword existingKeyword, Keyword adjustedKeyword) {
        super(source);
        this.existingKeyword = existingKeyword;
        this.adjustedKeyword = adjustedKeyword;
        this.oldValue = existingKeyword.getKeywordValue();
    }

    public Keyword getExistingKeyword() {
        return existingKeyword;
    }

    public Keyword getAdjustedKeyword() {
        return adjustedKeyword;
    }

    public String getOldValue() {
        return oldValue;
    }
}
