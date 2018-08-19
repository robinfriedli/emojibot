package core;

import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.annotation.Nullable;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class XmlPersister {

    private final Context context;
    private Document doc;

    public XmlPersister(Context context) {
        this.context = context;
        this.doc = getDocument();
    }

    public List<Element> getElements(String tagName) {
        return nodeListToElementList(doc.getElementsByTagName(tagName));
    }

    /**
     * Persist XmlElement to an XmlFile
     *
     * @param element to persist
     */
    public void persistElement(XmlElement element) {
        persistElement(element, null);
    }

    public void removeAll(String tagName) {
        Element rootElem = doc.getDocumentElement();
        nodeListToElementList(doc.getElementsByTagName(tagName)).forEach(rootElem::removeChild);
    }

    public void remove(XmlElement element) throws CommitException {
        Element elementToRemove = requireElement(element);
        elementToRemove.getParentNode().removeChild(elementToRemove);
    }

    public void setAttribute(XmlAttribute attribute) throws CommitException {
        Element element = requireElement(attribute.getParentElement());
        element.setAttribute(attribute.getAttributeName(), attribute.getValue());
    }

    public void setAttributes(XmlElement xmlElement, XmlAttribute... attributes) throws CommitException {
        setAttributes(xmlElement, Arrays.asList(attributes));
    }

    public void setAttributes(XmlElement xmlElement, List<XmlAttribute> attributes) throws CommitException {
        Element element = requireElement(xmlElement);
        for (XmlAttribute attribute : attributes) {
            if (xmlElement == attribute.getParentElement()) {
                element.setAttribute(attribute.getAttributeName(), attribute.getValue());
            } else {
                throw new CommitException("Could not set " + attribute.toString() + " on " + xmlElement.toString()
                    + ". Element is not attribute's parent");
            }
        }
    }

    public void setTextContent(XmlElement xmlElement) throws CommitException {
        Element element = requireElement(xmlElement);
        element.setTextContent(xmlElement.getTextContent());
    }

    public void addSubElements(XmlElement superElem, List<XmlElement> subElems) throws CommitException {
        Element element = requireElement(superElem);
        for (XmlElement subElem : subElems) {
            persistElement(subElem, element);
            if (!subElem.isPersisted()) {
                subElem.createShadow();
            }
        }
    }

    public void removeSubElements(XmlElement superElem, List<XmlElement> subElems) throws CommitException {
        Element element = requireElement(superElem);
        for (XmlElement subElem : subElems) {
            element.removeChild(requireSubElement(element, subElem));
        }
    }

    public boolean isSubElementOf(XmlElement subElem, XmlElement superElem) {
        Element element;
        try {
            element = requireElement(superElem);
            return !getElements(subElem, element).isEmpty();
        } catch (CommitException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void persistElement(XmlElement element, @Nullable Element superElem) {
        Element elem = doc.createElement(element.getTagName());

        List<XmlAttribute> attributes = element.getAttributes();
        for (XmlAttribute attribute : attributes) {
            elem.setAttribute(attribute.getAttributeName(), attribute.getValue());
        }

        if (element.hasTextContent()) {
            elem.setTextContent(element.getTextContent());
        }

        if (superElem != null) {
            superElem.appendChild(elem);
        } else {
            Element rootElem = doc.getDocumentElement();
            rootElem.appendChild(elem);
        }

        if (element.hasSubElements()) {
            for (XmlElement subElement : element.getSubElements()) {
                persistElement(subElement, elem);
            }
        }
    }

    private Element requireElement(XmlElement xmlElement) throws CommitException {
        List<Element> foundElems;
        if (!xmlElement.isSubElement()) {
            foundElems = getElements(xmlElement, null);
        } else {
            Element parentElem = requireElement(xmlElement.getParent());
            foundElems = getElements(xmlElement, parentElem);
        }

        if (foundElems.size() == 1) {
            return foundElems.get(0);
        } else if (foundElems.size() > 1) {
            throw new CommitException("Duplicate elements found for " + xmlElement.toString());
        } else {
            throw new CommitException("No element found in file for " + xmlElement.toString());
        }
    }

    public List<Element> getElements(XmlElement xmlElement, @Nullable Element superElem) throws CommitException {
        List<Element> elements;
        if (superElem != null) {
            elements = nodeListToElementList(superElem.getElementsByTagName(xmlElement.getTagName()));
        } else {
            elements = nodeListToElementList(doc.getElementsByTagName(xmlElement.getTagName()));
        }

        XmlElementShadow shadow = xmlElement.getShadow();
        if (shadow == null) {
            throw new CommitException(xmlElement.toString() + " does not have a shadow yet. Can not load from file.");
        }
        return elements.stream().filter(shadow::matches).collect(Collectors.toList());
    }

    private Element requireSubElement(Element superElem, XmlElement subElem) throws CommitException {
        List<Element> foundElems = getElements(subElem, superElem);

        if (foundElems.size() == 1) {
            return foundElems.get(0);
        } else if (foundElems.size() > 1) {
            throw new CommitException("Duplicate elements found for " + subElem.toString());
        } else {
            throw new CommitException("No element found in file for " + subElem.toString());
        }
    }

    public List<Element> find(String tagName, String attributeName, String attributeValue) {
        List<Element> elements = nodeListToElementList(doc.getElementsByTagName(tagName));
        return elements.stream().filter(elem -> elem.getAttribute(attributeName).equals(attributeValue)).collect(Collectors.toList());
    }

    public List<Element> find(String tagName, String textContent) {
        List<Element> elements = nodeListToElementList(doc.getElementsByTagName(tagName));
        return elements.stream().filter(elem -> elem.getTextContent().equals(textContent)).collect(Collectors.toList());
    }

    public List<Element> find(String tagName, String textContent, XmlElement parent) {
        try {
            Element element = requireElement(parent);
            List<Element> elements = nodeListToElementList(element.getElementsByTagName(tagName));
            return elements.stream().filter(elem -> elem.getTextContent().equals(textContent)).collect(Collectors.toList());
        } catch (CommitException e) {
            e.printStackTrace();
        }

        return Lists.newArrayList();
    }

    public List<Element> find(String tagName, String attrbuteName, String attributeValue, XmlElement parent) {
        try {
            Element element = requireElement(parent);
            List<Element> elements = nodeListToElementList(element.getElementsByTagName(tagName));
            return elements.stream().filter(elem -> elem.getAttribute(attrbuteName).equals(attributeValue)).collect(Collectors.toList());
        } catch (CommitException e) {
            e.printStackTrace();
        }

        return Lists.newArrayList();
    }

    public void writeToFile() throws CommitException {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(getFile());

            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    public void reloadDocument() {
        doc = getDocument();
    }

    private File getFile() throws CommitException {
        String path = context.getPath();

        File file = new File(path);
        if (file.exists()) {
            return file;
        } else if (context instanceof Context.BindableContext) {
            File fileToCopy = new File(context.getManager().getPath());
            if (fileToCopy.exists()) {
                try {
                    file.createNewFile();

                    FileChannel src = new FileInputStream(fileToCopy).getChannel();
                    FileChannel dest = new FileOutputStream(file).getChannel();
                    dest.transferFrom(src, 0, src.size());
                    src.close();
                    dest.close();

                    return file;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                throw new IllegalStateException("No file found for path defined in thread Context. Copying to new file for BindableContext failed");
            }
        } else {
            throw new IllegalStateException("Context is not bindable and no file has been found for specified path");
        }

        throw new CommitException("File loading failed");
    }

    public List<Element> getAllTopLevelElements() {
        return getChildren(doc.getDocumentElement());
    }

    public List<Element> getChildren(Element parent) {
        NodeList childNodes = parent.getChildNodes();
        List<Element> elements = Lists.newArrayList();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node instanceof Element) {
                elements.add((Element) node);
            }
        }

        return elements;
    }

    public Document getDocument() {
        try {
            File xml = getFile();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(xml);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<Element> nodeListToElementList(NodeList nodeList) {
        List<Element> elements = Lists.newArrayList();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }

        return elements;
    }

}
