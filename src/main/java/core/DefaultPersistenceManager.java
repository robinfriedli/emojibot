package core;

import com.google.common.collect.Lists;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultPersistenceManager {

    private Context context;
    private XmlPersister xmlPersister;

    public void initialize(Context context) {
        this.context = context;
        xmlPersister = new XmlPersister(context);
    }

    public List<XmlElement> getAllElements() {
        List<XmlElement> xmlElements = Lists.newArrayList();
        List<Element> allTopLevelElements = xmlPersister.getAllTopLevelElements();

        for (Element topElement : allTopLevelElements) {
            xmlElements.add(instantiateBaseXmlElement(topElement));
        }

        return xmlElements;
    }

    private XmlElement instantiateBaseXmlElement(Element element) {
        List<Element> subElements = xmlPersister.getChildren(element);
        List<XmlElement> instantiatedSubElems = Lists.newArrayList();
        for (Element subElement : subElements) {
            instantiatedSubElems.add(instantiateBaseXmlElement(subElement));
        }

        NamedNodeMap attributes = element.getAttributes();
        Map<String, String> attributeMap = new HashMap<>();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attribute = attributes.item(i);
            attributeMap.put(attribute.getNodeName(), attribute.getNodeValue());
        }

        Node childNode = element.getChildNodes().item(0);
        String textContent = childNode != null && childNode.getNodeType() == Node.TEXT_NODE && !childNode.getTextContent().trim().equals("") ? childNode.getTextContent() : "";


        return new BaseXmlElement(element.getTagName(), attributeMap, instantiatedSubElems, textContent, XmlElement.State.CLEAN, context);
    }

    public void buildTree(List<XmlElement> elements) {
        for (XmlElement element : elements) {
            if (element.hasSubElements()) {
                for (XmlElement subElement : element.getSubElements()) {
                    setParent(subElement, element);
                }
            }
        }
    }

    private void setParent(XmlElement child, XmlElement parent) {
        child.setParent(parent);

        if (child.hasSubElements()) {
            for (XmlElement subElement : child.getSubElements()) {
                setParent(subElement, child);
            }
        }
    }

    public Context getContext() {
        return context;
    }

    public XmlPersister getXmlPersister() {
        return xmlPersister;
    }

    public void commitElementChanges(ElementChangingEvent event) throws CommitException {
        XmlElement element = event.getSource();
        List<XmlAttribute> attributesToChange = event.getChangedAttributes() != null
                ? event.getChangedAttributes().stream().map(ValueChangingEvent::getNewValue).collect(Collectors.toList())
                : Lists.newArrayList();

        List<XmlElement> subElementsToAdd = event.getAddedSubElements();
        List<XmlElement> subElementsToRemove  = event.getRemovedSubElements();

        if (subElementsToAdd != null && !subElementsToAdd.isEmpty()) xmlPersister.addSubElements(element, subElementsToAdd);
        if (!attributesToChange.isEmpty()) xmlPersister.setAttributes(element, attributesToChange);
        if (element.textContentChanged()) xmlPersister.setTextContent(element);
        if (subElementsToRemove != null && !subElementsToRemove.isEmpty()) xmlPersister.removeSubElements(element, subElementsToRemove);
    }

    public void write() throws CommitException {
        xmlPersister.writeToFile();
        xmlPersister.reloadDocument();
    }

    public void reload() {
        xmlPersister.reloadDocument();
    }

    public void castElement(XmlElement target, XmlElement source) {
        if (target.matchesStructure(source)) {
            List<ValueChangingEvent<XmlAttribute>> changedAttributes = Lists.newArrayList();
            ValueChangingEvent<String> changedTextContent = null;

            for (XmlAttribute attribute : source.getAttributes()) {
                XmlAttribute existingAttribute = target.getAttribute(attribute.getAttributeName());
                if (!attribute.getValue().equals(existingAttribute.getValue())) {
                    changedAttributes.add(new ValueChangingEvent<>(target, existingAttribute, attribute));
                }
            }

            if (!target.getTextContent().equals(source.getTextContent())) {
                changedTextContent = new ValueChangingEvent<>(target, target.getTextContent(), source.getTextContent());
            }

            List<XmlElement> addedSubElements = Lists.newArrayList();
            List<XmlElement> removedSubElements = Lists.newArrayList();

            for (XmlElement subElement : source.getSubElements()) {
                if (!target.hasSubElement(subElement.getId())) {
                    addedSubElements.add(subElement);
                }
            }

            for (XmlElement subElement : target.getSubElements()) {
                if (!source.hasSubElement(subElement.getId())) {
                    removedSubElements.add(subElement);
                } else {
                    XmlElement newSubElement = source.requireSubElement(subElement.getId());
                    castElement(subElement, newSubElement);
                }
            }

            ElementChangingEvent elementChangingEvent =
                new ElementChangingEvent(target, changedAttributes, changedTextContent, addedSubElements, removedSubElements);

            if (!elementChangingEvent.isEmpty()) {
                target.addChange(elementChangingEvent);
            }
        } else {
            throw new PersistException("Could not cast element. Incompatible structures.");
        }
    }

}
