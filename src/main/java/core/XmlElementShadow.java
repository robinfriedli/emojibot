package core;

import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that represents the current representation of the {@link XmlElement} in the XML file.
 * Gets created after committing a new XmlElement or creating an XmlElement with {@link XmlElement.State#CLEAN}
 * (when loading all XmlElements on startup / initializing a {@link Context}) and is updated after committing an
 * {@link ElementChangingEvent}
 */
public class XmlElementShadow {

    private final XmlElement source;

    private final Map<String, String> attributes;

    private String textContent;

    public XmlElementShadow(XmlElement source) {
        this.source = source;
        this.attributes = new HashMap<>();
        this.textContent = source.getTextContent();

        buildAttributeMap();
    }

    public void update() {
        buildAttributeMap();
        this.textContent = source.getTextContent();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getTextContent() {
        return textContent;
    }

    public boolean matches(Element element) {
        Set<String> attributeNames = attributes.keySet();
        return attributeNames.stream().allMatch(name -> element.getAttribute(name).equals(attributes.get(name)))
            && (!source.hasTextContent() || element.getTextContent().equals(textContent));
    }

    private void buildAttributeMap() {
        List<XmlAttribute> attributeInstances = source.getAttributes();
        for (XmlAttribute attributeInstance : attributeInstances) {
            attributes.put(attributeInstance.getAttributeName(), attributeInstance.getValue());
        }
    }
}
