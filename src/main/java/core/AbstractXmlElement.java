package core;

import com.google.common.collect.Lists;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract class to extend for classes you wish to able to persist to XML file.
 * Classes that extend this class can be persisted via the {@link PersistenceManager}
 */
public abstract class AbstractXmlElement implements XmlElement {

    private XmlElement parent;

    private final Context context;

    private final String tagName;

    private final List<XmlAttribute> attributes;

    private final List<XmlElement> subElements;

    private String textContent;

    private State state;

    private List<ElementChangingEvent> changes = Lists.newArrayList();

    private boolean locked = false;

    private XmlElementShadow shadow;

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), "", State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, State state, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), "", state, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, String textContent, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), textContent, State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, String textContent, State state, Context context) {
        this(tagName, attributeMap, Lists.newArrayList(), textContent, state, context);
    }

    public AbstractXmlElement(String tagName, Map<String, String> attributeMap, List<XmlElement> subElements, Context context) {
        this(tagName, attributeMap, subElements, "", State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              State state,
                              Context context) {
        this(tagName, attributeMap, subElements, "", state, context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              String textContent,
                              Context context) {
        this(tagName, attributeMap, subElements, textContent, State.CONCEPTION, context);
    }

    public AbstractXmlElement(String tagName,
                              Map<String, String> attributeMap,
                              List<XmlElement> subElements,
                              String textContent,
                              State state,
                              Context context) {
        this.tagName = tagName;
        this.subElements = subElements;
        this.textContent = textContent;
        this.state = state;
        this.context = context;

        List<XmlAttribute> attributes = Lists.newArrayList();
        for (String attributeName : attributeMap.keySet()) {
            attributes.add(new XmlAttribute(this, attributeName, attributeMap.get(attributeName)));
        }
        this.attributes = attributes;

        if (state == State.CONCEPTION) {
            Transaction transaction = context.getTransaction();
            if (transaction == null) {
                throw new PersistException("Context has no transaction. Use Context#invoke.");
            }
            subElements.forEach(sub -> sub.setParent(this));
            transaction.addChange(new ElementCreatedEvent(this));
        } else if (state == State.CLEAN) {
            // when an XmlElement is instantiated with State CLEAN that means it was created while initializing a new Context
            // via DefaultPersistenceManager#getAllElements and thus already exists in the XmlFile. In this case we need
            // to create an XmlElementShadow with the current state of the XmlElement.
            shadow = new XmlElementShadow(this);
        } else {
            throw new PersistException("New XmlElements should be instantiated with either State CONCEPTION (default) " +
                "or CLEAN (if the XmlElement was loaded from the XmlFile)");
        }
    }

    @Override
    @Nullable
    public abstract String getId();

    @Override
    public void setParent(XmlElement parent) {
        if (!isPersisted()) {
            // this element has not been persisted yet, so it's safe to put it as a child
            this.parent = parent;
        } else if (this.parent != null && this.parent != parent && this.parent.getState() != State.DELETION) {
            // this element already has a different parent, remove it from the old parent
            this.parent.removeSubElement(this);
            this.parent = parent;
        } else if (parent.isPersisted()) {
            // both parent and child already exist in the XML file
            // TODO maybe don't verify if it ever makes sense to turn an element into a subelement
            // already do set the parent before verifying to help finding the element
            this.parent = parent;
            //verify that this is actually a subElement of parent
            XmlPersister xmlPersister = context.getPersistenceManager().getXmlPersister();
            boolean subElementOf = xmlPersister.isSubElementOf(this, parent);
            if (!subElementOf) {
                this.parent = null;
                throw new PersistException("Can't set parent. " + toString() + " is not a child of " + parent.toString());
            }
        } else {
            // this element already exists but it's parent doesn't
            this.parent = parent;
        }
    }

    @Override
    public XmlElement getParent() {
        return parent;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public boolean isSubElement() {
        return parent != null;
    }

    @Override
    public boolean isPersisted() {
        return shadow != null;
    }

    @Override
    public boolean checkDuplicates(XmlElement element) {
        if (!this.getTagName().equals(element.getTagName())) return false;
        if (this.getId() == null || element.getId() == null) return false;
        return this.getId().equals(element.getId());
    }

    @Override
    public String getTagName() {
        return tagName;
    }

    @Override
    public List<XmlAttribute> getAttributes() {
        return attributes;
    }

    @Override
    public XmlAttribute getAttribute(String attributeName) {
        List<XmlAttribute> foundAttributes = attributes.stream()
            .filter(a -> a.getAttributeName().equals(attributeName))
            .collect(Collectors.toList());

        if (foundAttributes.size() == 1) {
            return foundAttributes.get(0);
        } else if (foundAttributes.size() > 1) {
            throw new IllegalStateException("Duplicate attribute: " + attributeName + " on element " + toString());
        } else {
            throw new IllegalStateException("No attribute " + attributeName + " on element " + toString());
        }
    }

    @Override
    public void setAttribute(String attribute, String value) {
        XmlAttribute attributeToChange = getAttribute(attribute);
        attributeToChange.setValue(value);
    }

    @Override
    public boolean hasAttribute(String attributeName) {
        return getAttributes().stream().anyMatch(attribute -> attribute.getAttributeName().equals(attributeName));
    }

    @Override
    public void addSubElement(XmlElement element) {
        addSubElements(Lists.newArrayList(element));
    }

    @Override
    public void addSubElements(List<XmlElement> elements) {
        elements.forEach(elem -> elem.setParent(this));
        addChange(new ElementChangingEvent(this, elements, null));
    }

    @Override
    public void addSubElements(XmlElement... elements) {
        addSubElements(Arrays.asList(elements));
    }

    @Override
    public void removeSubElement(XmlElement element) {
        removeSubElements(Lists.newArrayList(element));
    }

    @Override
    public void removeSubElements(List<XmlElement> elements) {
        addChange(new ElementChangingEvent(this, null, elements));
    }

    @Override
    public void removeSubElements(XmlElement... elements) {
        removeSubElements(Arrays.asList(elements));
    }

    @Override
    public List<XmlElement> getSubElements() {
        return this.subElements;
    }

    @Override
    public <E extends XmlElement> List<E> getSubElementsWithType(Class<E> type) {
        return getSubElements().stream().filter(type::isInstance).map(type::cast).collect(Collectors.toList());
    }


    @Override
    public XmlElement getSubElement(String id) {
        List<XmlElement> foundSubElements = getSubElements().stream()
            .filter(subElem -> subElem.getId() != null && subElem.getId().equals(id))
            .collect(Collectors.toList());

        if (foundSubElements.size() == 1) {
            return foundSubElements.get(0);
        } else if (foundSubElements.size() > 1) {
            throw new IllegalStateException("Multiple SubElements found for id " + id + ". Id's must be unique");
        } else {
            return null;
        }
    }

    @Override
    public XmlElement requireSubElement(String id) throws IllegalStateException {
        XmlElement subElement = getSubElement(id);

        if (subElement != null) {
            return subElement;
        } else {
            throw new IllegalStateException("No SubElement found for id " + id);
        }
    }

    @Override
    public boolean hasSubElements() {
        return subElements != null && !subElements.isEmpty();
    }

    @Override
    public boolean hasSubElement(String id) {
        return getSubElements().stream().anyMatch(subElem -> subElem.getId() != null && subElem.getId().equals(id));
    }

    @Override
    public String getTextContent() {
        return textContent;
    }

    @Override
    public void setTextContent(String textContent) {
        String oldValue = String.valueOf(getTextContent());
        ValueChangingEvent<String> valueChangingEvent = new ValueChangingEvent<>(this, oldValue, textContent);
        addChange(new ElementChangingEvent(this, valueChangingEvent));
    }
    @Override
    public boolean hasTextContent() {
        return textContent != null && !textContent.equals("");
    }

    @Override
    public boolean matchesStructure(XmlElement elementToCompare) {
        if (!elementToCompare.getTagName().equals(getTagName())) return false;
        return elementToCompare.getAttributes().stream().allMatch(attribute -> this.hasAttribute(attribute.getAttributeName()))
            && getAttributes().stream().allMatch(attribute -> elementToCompare.hasAttribute(attribute.getAttributeName()));
    }

    @Override
    public void delete() {
        Transaction transaction = context.getTransaction();

        if (transaction == null) {
            throw new PersistException("Context has no transaction. Use Context#invoke");
        }

        if (isSubElement()) {
            getParent().removeSubElement(this);
        } else {
            transaction.addChange(new ElementDeletingEvent(this, getState()));
        }
    }

    @Override
    public void addChange(ElementChangingEvent change) {
        if (!isLocked()) {
            Transaction transaction = context.getTransaction();

            if (transaction == null) {
                throw new PersistException("Context has no transaction. Use Context#exectePersistTask");
            }

            setState(State.TOUCHED);
            changes.add(change);
            transaction.addChange(change);
        } else {
            throw new PersistException("Unable to add Change. " + toString() + " is locked, probably duplicate.");
        }
    }

    @Override
    public void removeChange(ElementChangingEvent change) {
        changes.remove(change);

        if (!hasChanges()) {
            setState(State.CLEAN);
        }
    }

    @Override
    public void applyChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException {
        applyChange(change, false);
    }

    @Override
    public void revertChange(ElementChangingEvent change) throws UnsupportedOperationException, PersistException {
        applyChange(change, true);
    }

    @Override
    public List<ElementChangingEvent> getChanges() {
        if (getState() != State.TOUCHED) {
            throw new UnsupportedOperationException("Trying to call getChanges() on an XmlElement that is not in State TOUCHED but "
                + getState().toString());
        }
        return this.changes;
    }

    @Override
    public boolean hasChanges() {
        return changes != null && !changes.isEmpty();
    }

    @Override
    public ElementChangingEvent getFirstChange() {
        return hasChanges() ? getChanges().get(0) : null;
    }

    @Override
    public ElementChangingEvent getLastChange() {
        List<ElementChangingEvent> changes = getChanges();
        return hasChanges() ? changes.get(changes.size() - 1) : null;
    }

    @Override
    public ValueChangingEvent<XmlAttribute> getFirstAttributeChange(String attributeName) {
        if (!hasAttribute(attributeName)) {
            throw new IllegalArgumentException(toString() + " does not have an attribute named " + attributeName);
        }

        if (hasChanges()) {
            for (ElementChangingEvent change : getChanges()) {
                if (change.attributeChanged(attributeName)) {
                    return change.getAttributeChange(attributeName);
                }
            }
        }

        return null;
    }

    @Override
    public ValueChangingEvent<XmlAttribute> getLastAttributeChange(String attributeName) {
        if (!hasAttribute(attributeName)) {
            throw new IllegalArgumentException(toString() + " does not have an attribute named " + attributeName);
        }

        ValueChangingEvent<XmlAttribute> attributeChange = null;
        if (hasChanges()) {
            for (ElementChangingEvent change : getChanges()) {
                if (change.attributeChanged(attributeName)) {
                    attributeChange = change.getAttributeChange(attributeName);
                }
            }
        }

        return attributeChange;
    }

    @Override
    public boolean attributeChanged(String attributeName) {
        return getFirstAttributeChange(attributeName) != null;
    }

    @Override
    public ValueChangingEvent<String> getFirstTextContentChange() {
        for (ElementChangingEvent change : getChanges()) {
            if (change.textContentChanged()) {
                return change.getChangedTextContent();
            }
        }

        return null;
    }

    @Override
    public boolean textContentChanged() {
        return getFirstTextContentChange() != null;
    }

    @Override
    public void clearChanges() {
        this.changes.clear();
    }

    @Override
    public void lock() {
        locked = true;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public XmlElementShadow getShadow() {
        return shadow;
    }

    @Override
    public void updateShadow() {
        shadow.update();
    }

    @Override
    public void createShadow() {
        if (shadow == null) {
            shadow = new XmlElementShadow(this);
        } else {
            throw new PersistException(toString() + " already has a shadow");
        }
    }

    @Override
    public State getState() {
        return state;
    }

    @Override
    public void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return "XmlElement<" + getTagName() + ">:" + getId();
    }

    private void applyChange(ElementChangingEvent change, boolean isRollback) {
        Transaction transaction = context.getTransaction();

        if (transaction == null) {
            throw new PersistException("Cannot persist changes, context has no transaction. Use Context#invoke");
        }

        if (change.getSource() != this) {
            throw new UnsupportedOperationException("Source of ElementChangingEvent is not this XmlElement. Change can't be applied.");
        }

        if (change.getChangedAttributes() != null) {
            for (ValueChangingEvent<XmlAttribute> changedAttribute : change.getChangedAttributes()) {
                XmlAttribute oldValue = changedAttribute.getOldValue();
                XmlAttribute newValue = changedAttribute.getNewValue();

                if (oldValue.getAttributeName().equals(newValue.getAttributeName())) {
                    XmlAttribute attributeToChange = getAttribute(oldValue.getAttributeName());
                    if (isRollback) {
                        attributeToChange.revertChange(changedAttribute);
                    } else {
                        attributeToChange.applyChange(changedAttribute);
                    }
                } else {
                    throw new UnsupportedOperationException("Cannot apply malformed ValueChangingEvent. " +
                        "OldValue and newValue do not refer to the same attribute");
                }
            }
        }

        if (change.getAddedSubElements() != null) {
            if (isRollback) {
                subElements.removeAll(change.getAddedSubElements());
            } else {
                subElements.addAll(change.getAddedSubElements());
            }
        }

        if (change.getRemovedSubElements() != null) {
            if (isRollback) {
                subElements.addAll(change.getRemovedSubElements());
            } else {
                subElements.removeAll(change.getRemovedSubElements());
            }
        }

        if (change.getChangedTextContent() != null) {
            if (isRollback) {
                this.textContent = change.getChangedTextContent().getOldValue();
            } else {
                this.textContent = change.getChangedTextContent().getNewValue();
            }
        }
    }


}
