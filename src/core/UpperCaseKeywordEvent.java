package core;

import java.util.List;

import api.Emoji;

public class UpperCaseKeywordEvent extends EmojiChangingEvent {

    public UpperCaseKeywordEvent(List<KeywordChangingEvent> changedKeywords, Emoji sourceEmoji) {
        super(null, null, null, changedKeywords, sourceEmoji);
    }

}
