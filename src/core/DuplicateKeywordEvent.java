package core;

import java.util.List;

import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;

public class DuplicateKeywordEvent extends EmojiChangingEvent {

    private final Keyword replacement;
    private final List<Keyword> duplicates;

    public DuplicateKeywordEvent(Keyword replacement,
                                 List<Keyword> duplicates,
                                 Emoji sourceEmoji) {
        super(null, Lists.newArrayList(replacement), duplicates, null, sourceEmoji);
        this.replacement = replacement;
        this.duplicates = duplicates;
    }

    public Keyword getReplacement() {
        return replacement;
    }

    public List<Keyword> getDuplicates() {
        return duplicates;
    }
}
