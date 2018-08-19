package core;

public abstract class Event {

    private final XmlElement source;

    private boolean applied = false;

    public Event(XmlElement source) {
        this.source = source;
    }

    public XmlElement getSource() {
        return this.source;
    }

    public abstract void apply();

    public abstract void revert();

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public boolean isApplied() {
        return applied;
    }
}
