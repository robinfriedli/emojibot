package core;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class ElementChangingEvent extends Event {

    @Nullable
    private final List<ValueChangingEvent<XmlAttribute>> changedAttributes;

    @Nullable
    private final ValueChangingEvent<String> changedTextContent;

    @Nullable
    private final List<XmlElement> addedSubElements;

    @Nullable
    private final List<XmlElement> removedSubElements;

    public ElementChangingEvent(XmlElement source, List<ValueChangingEvent<XmlAttribute>> changedAttributes) {
        this(source, changedAttributes, null, null, null);
    }

    public ElementChangingEvent(XmlElement source, ValueChangingEvent<String> changedTextContent) {
        this(source, null, changedTextContent, null, null);
    }

    public ElementChangingEvent(XmlElement source,
                                List<ValueChangingEvent<XmlAttribute>> changedAttributes,
                                ValueChangingEvent<String> changedTextContent) {
        this(source, changedAttributes, changedTextContent, null, null);
    }

    public ElementChangingEvent(XmlElement source,
                                List<XmlElement> addedSubElements,
                                List<XmlElement> removedSubElements) {
        this(source, null, null, addedSubElements, removedSubElements);
    }

    public ElementChangingEvent(XmlElement source,
                                @Nullable List<ValueChangingEvent<XmlAttribute>> changedAttributes,
                                @Nullable ValueChangingEvent<String> changedTextContent,
                                @Nullable List<XmlElement> addedSubElements,
                                @Nullable List<XmlElement> removedSubElements) {
        super(source);
        this.changedAttributes = changedAttributes;
        this.changedTextContent = changedTextContent;
        this.addedSubElements = addedSubElements;
        this.removedSubElements = removedSubElements;
    }

    @Nullable
    public List<ValueChangingEvent<XmlAttribute>> getChangedAttributes() {
        return changedAttributes;
    }

    @Nullable
    public ValueChangingEvent<String> getChangedTextContent() {
        return changedTextContent;
    }

    @Nullable
    public List<XmlElement> getAddedSubElements() {
        return addedSubElements;
    }

    @Nullable
    public List<XmlElement> getRemovedSubElements() {
        return removedSubElements;
    }

    @Override
    public void apply() {
        if (isApplied()) {
            throw new PersistException("Change has already been applied");
        } else {
            getSource().applyChange(this);
            setApplied(true);
            getSource().getContext().getManager().fireElementChanging(this);
        }
    }

    @Override
    public void revert() {
        if (isApplied()) {
            getSource().revertChange(this);
        }
    }

    public boolean isEmpty() {
        return (changedAttributes == null || changedAttributes.isEmpty())
            && (changedTextContent == null)
            && (addedSubElements == null || addedSubElements.isEmpty())
            && (removedSubElements == null || removedSubElements.isEmpty());
    }

    public boolean attributeChanged(String attributeName) {
        if (changedAttributes != null) {
            return changedAttributes.stream().anyMatch(change -> change.getOldValue().getAttributeName().equals(attributeName));
        }

        return false;
    }

    public ValueChangingEvent<XmlAttribute> getAttributeChange(String attributeName) {
        if (changedAttributes != null) {
            List<ValueChangingEvent<XmlAttribute>> foundChanges = changedAttributes.stream()
                .filter(change -> change.getOldValue().getAttributeName().equals(attributeName))
                .collect(Collectors.toList());

            if (foundChanges.size() == 1) {
                return foundChanges.get(0);
            } else if (foundChanges.size() > 1) {
                // this should never happen since the event is generated when calling XmlAttribute#setValue
                throw new IllegalStateException("Multiple changes recorded for attribute " + attributeName + " within the same event");
            }
        }

        return null;
    }

    public boolean textContentChanged() {
        return changedTextContent != null;
    }

}
