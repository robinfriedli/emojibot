package core;

public class ElementCreatedEvent extends Event {

    private boolean isDuplicate = false;

    public ElementCreatedEvent(XmlElement element) {
        super(element);

        String id = element.getId();
        if (id != null) {
            XmlElement existingElement = element.getContext().getElement(id);
            if (existingElement != null) {
                throw new PersistException("There already is an XmlElement with id " + id);
            }
        }
    }

    @Override
    public void apply() {
        if (!isDuplicate) {
            if (isApplied()) {
                throw new PersistException("Change has already been applied");
            } else {
                XmlElement source = getSource();
                if (!source.isSubElement()) {
                    source.getContext().addElement(source);
                    setApplied(true);
                    getSource().getContext().getManager().fireElementCreating(this);
                }
            }
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            XmlElement source = getSource();
            source.getContext().removeElement(source);
        }
    }
}
