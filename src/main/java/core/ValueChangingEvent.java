package core;

public class ValueChangingEvent<V> extends Event {

    private final V oldValue;
    private final V newValue;

    public ValueChangingEvent(XmlElement source, V oldValue, V newValue) {
        super(source);
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public V getOldValue() {
        return oldValue;
    }

    public V getNewValue() {
        return newValue;
    }

    @Override
    public void apply() {
        // do nothing, this is a helper Class for ElementChangingEvent
    }

    @Override
    public void revert() {
        // do nothing, this is a helper Class for ElementChangingEvent
    }
}
