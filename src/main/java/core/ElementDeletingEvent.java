package core;

public class ElementDeletingEvent extends Event {

    private final XmlElement.State previousState;

    public ElementDeletingEvent(XmlElement element, XmlElement.State previousState) {
        super(element);
        this.previousState = previousState;
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            getSource().setState(XmlElement.State.DELETION);
            setApplied(true);
            getSource().getContext().getManager().fireElementDeleting(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            getSource().setState(previousState);
        }
    }
}
