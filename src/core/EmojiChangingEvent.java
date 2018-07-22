package core;


import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import api.Emoji;
import api.Keyword;

/**
 * class documenting the changes made to an emoji during one transaction
 */
public class EmojiChangingEvent extends Event {

    @Nullable
    private final Boolean changedRandomTag;

    @Nullable
    private final List<Keyword> addedKeywords;

    @Nullable
    private final List<Keyword> removedKeywords;

    @Nullable
    private final Map<String, Boolean> changedKeywords;

    public EmojiChangingEvent(@Nullable Boolean changedRandomTag,
                              @Nullable List<Keyword> addedKeywords,
                              @Nullable List<Keyword> removedKeywords,
                              @Nullable Map<String, Boolean> changedKeywords,
                              Emoji sourceEmoji) {
        super(sourceEmoji);
        this.changedRandomTag = changedRandomTag;
        this.addedKeywords = addedKeywords;
        this.removedKeywords = removedKeywords;
        this.changedKeywords = changedKeywords;
    }

    @Nullable
    public Boolean getRandomTag() {
        return this.changedRandomTag;
    }

    @Nullable
    public List<Keyword> getAddedKeywords() {
        return this.addedKeywords;
    }

    @Nullable
    public List<Keyword> getRemovedKeywords() {
        return this.removedKeywords;
    }

    @Nullable
    public Map<String, Boolean> getChangedKeywords() {
        return this.changedKeywords;
    }
}
