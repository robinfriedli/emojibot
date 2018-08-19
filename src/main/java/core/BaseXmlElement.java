package core;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Default implementation of {@link XmlElement} that can automatically be generated when load the XML file
 * It is recommended that you instantiate your own classes by overriding {@link DefaultPersistenceManager#getAllElements()}
 * so that you can define an id to load the XmlElement form the Context more easily and obviously use the methods of your
 * own class
 */
public class BaseXmlElement extends AbstractXmlElement {

    public BaseXmlElement(String tagName, Map<String, String> attributeMap, List<XmlElement> subElements, String textContent, Context context) {
        super(tagName, attributeMap, subElements, textContent, context);
    }

    public BaseXmlElement(String tagName, Map<String, String> attributeMap, List<XmlElement> subElements, String textContent, State state, Context context) {
        super(tagName, attributeMap, subElements, textContent, state, context);
    }

    @Nullable
    @Override
    public String getId() {
        return null;
    }
}
