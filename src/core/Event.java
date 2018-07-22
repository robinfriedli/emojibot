package core;

import api.Emoji;

public class Event {

    private final Emoji source;

    public Event(Emoji source) {
        this.source = source;
    }

    public Emoji getSource() {
        return this.source;
    }
}
