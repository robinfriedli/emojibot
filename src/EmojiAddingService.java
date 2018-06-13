import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmojiAddingService {

    private EmojiLoadingService emojiLoadingService = new EmojiLoadingService();

    public void addEmojis(String[] emojis) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        for (String emoji : emojis) {
            if (!emojiExists(doc, emoji)) {
                Element emojiElem = doc.createElement("emoji");
                emojiElem.setAttribute("value", emoji);
                rootElem.appendChild(emojiElem);
            }
        }

        writeToFile(doc);
    }

    public void addEmojis(String[] emojis, String[] keywords, String[] replaceTags) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        for (String emoji : emojis) {
            Element emojiElem;

            //only create new emoji if there isn't one with the same value, else load the existing emoji
            if (!emojiExists(doc, emoji)) {
                emojiElem = doc.createElement("emoji");
                emojiElem.setAttribute("value", emoji);
            } else {
                emojiElem = getEmojiElem(doc, emoji);
            }

            for (int i = 0; i < keywords.length; i++) {
                Element keywordElem;

                //only create new keyword if it doesn't already exist on the same emoji, else load existing keyword
                // and adjust replace flag
                if (!keywordExists(emojiElem, keywords[i])) {
                    keywordElem = doc.createElement("keyword");
                } else {
                    keywordElem = getKeywordElem(emojiElem, keywords[i]);
                }

                keywordElem.setAttribute("replace", replaceTags[i]);
                keywordElem.setTextContent(keywords[i]);

                emojiElem.appendChild(keywordElem);
            }

            rootElem.appendChild(emojiElem);
        }

        writeToFile(doc);
    }

    public void removeEmojis(List<String> emojisToRemove) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();
        List<Element> emojiElems = getEmojiElems(doc, emojisToRemove);

        for (Element emoji : emojiElems) {
            String emojiValue = emoji.getAttribute("value");

            if (emojisToRemove.contains(emojiValue)) {
                rootElem.removeChild(emoji);
            }
        }

        writeToFile(doc);
    }

    public void removeKeywords(List<String> emojis, List<String> keywordsToRemove) {
        Document doc = emojiLoadingService.getDocument();
        List<Element> emojiElems = getEmojiElems(doc, emojis);

        for (Element emoji : emojiElems) {
            String emojiValue = emoji.getAttribute("value");

            if (emojis.contains(emojiValue)) {
                List<Element> keywordElems = getKeywordElems(emoji, keywordsToRemove);

                for (Element keyword : keywordElems) {
                    String keywordValue = keyword.getTextContent();

                    if (keywordsToRemove.contains(keywordValue)) {
                        emoji.removeChild(keyword);
                    }
                }
            }
        }

        writeToFile(doc);
    }

    private boolean emojiExists(Document doc, String emojiString) {
        return getEmojiElem(doc, emojiString) != null;
    }

    private boolean keywordExists(Element emojiElem, String keywordString) {
        return getKeywordElem(emojiElem, keywordString) != null;
    }

    private List<Element> getEmojiElems(Document doc, List<String> emojiStrings) {
        List<Element> elems = new ArrayList<>();
        NodeList emojiList = doc.getElementsByTagName("emoji");

        for (int i = 0; i < emojiList.getLength(); i++) {
            Element emoji = (Element) emojiList.item(i);
            String emojiValue = emoji.getAttribute("value");

            if (emojiStrings.contains(emojiValue)) {
                elems.add(emoji);
            }
        }

        return elems;
    }

    private Element getEmojiElem(Document doc, String emojiString) {
        NodeList emojiList = doc.getElementsByTagName("emoji");

        for (int i = 0; i < emojiList.getLength(); i++) {
            Element emoji = (Element) emojiList.item(i);
            String emojiValue = emoji.getAttribute("value");

            if (emojiString.equals(emojiValue)) {
                return emoji;
            }
        }

        return null;
    }

    private List<Element> getKeywordElems(Element emojiElem, List<String> keywordStrings) {
        List<Element> elems = new ArrayList<>();
        NodeList keywordList = emojiElem.getElementsByTagName("keyword");

        for (int i = 0; i < keywordList.getLength(); i++) {
            Element keyword = (Element) keywordList.item(i);
            String keywordValue = keyword.getTextContent();

            if (keywordStrings.contains(keywordValue)) {
                elems.add(keyword);
            }
        }

        return elems;
    }

    private Element getKeywordElem(Element emojiElem, String keywordString) {
        NodeList keywordList = emojiElem.getElementsByTagName("keyword");

        for (int i = 0; i < keywordList.getLength(); i++) {
            Element keyword = (Element) keywordList.item(i);
            String keywordValue = keyword.getTextContent();

            if (keywordValue.equals(keywordString)) {
                return keyword;
            }
        }

        return null;
    }

    private void writeToFile(Document doc) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("./resources/emojis.xml"));

            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }

}
