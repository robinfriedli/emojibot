package core;

import com.google.common.collect.Lists;

public class XmlAttribute {

    private final XmlElement parentElement;
    private final String attributeName;
    private String value;

    public XmlAttribute(XmlElement parentElement, String attributeName) {
        this.parentElement = parentElement;
        this.attributeName = attributeName;
    }

    public XmlAttribute(XmlElement parentElement, String attributeName, String value) {
        this.parentElement = parentElement;
        this.attributeName = attributeName;
        this.value = value;
    }

    public XmlElement getParentElement() {
        return this.parentElement;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    public void setValue(String value) {
        XmlAttribute newValue = new XmlAttribute(parentElement, attributeName, value);
        ValueChangingEvent<XmlAttribute> changedAttribute = new ValueChangingEvent<>(parentElement, this, newValue);
        parentElement.addChange(new ElementChangingEvent(parentElement, Lists.newArrayList(changedAttribute)));
    }

    public String getValue() {
        return this.value;
    }

    public void applyChange(ValueChangingEvent<XmlAttribute> change) throws UnsupportedOperationException {
        applyChange(change, false);
    }

    public void revertChange(ValueChangingEvent<XmlAttribute> change) {
        applyChange(change, true);
    }

    @Override
    public String toString() {
        return "XmlAttribute:" + getAttributeName() + "@" + getParentElement().getTagName();
    }

    private void applyChange(ValueChangingEvent<XmlAttribute> change, boolean isRollback) {
        if (change.getSource() != parentElement) {
            throw new UnsupportedOperationException("Change can't be applied to XmlAttribute since the source of the change is not its parent");
        }

        if (change.getOldValue() != this) {
            throw new UnsupportedOperationException("Change can't be applied to this XmlAttribute since the change does not refer to this attribute");
        }

        if (!change.getNewValue().getAttributeName().equals(this.getAttributeName())) {
            throw new UnsupportedOperationException("Cannot apply malformed ValueChangingEvent. " +
                "OldValue and newValue do not refer to the same attribute type");
        }

        this.value = isRollback ? change.getOldValue().getValue() : change.getNewValue().getValue();
    }

}
