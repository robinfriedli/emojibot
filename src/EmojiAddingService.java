import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

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

    public void mergeDuplicateEmojis(List<String> duplicateEmojis) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        NodeList emojis = doc.getElementsByTagName("emoji");
        List<Element> newEmojis = Lists.newArrayList();

        //create new emoji elements for each duplicate emoji
        for (String duplicateEmoji : duplicateEmojis) {
            Element emoji = doc.createElement("emoji");
            emoji.setAttribute("value", duplicateEmoji);

            newEmojis.add(emoji);
        }

        for (Element newEmoji : newEmojis) {
            String emojiValue = newEmoji.getAttribute("value");
            List<Element> otherEmojis = Lists.newArrayList();
            List<Element> keywords = Lists.newArrayList();

            //get other emojis with the same value
            for (int i = 0; i < emojis.getLength(); i++) {
                Element emojiElem = (Element) emojis.item(i);

                if (emojiElem.getAttribute("value").equals(emojiValue)) {
                    otherEmojis.add(emojiElem);
                }
            }

            //get keywords of those emojis and remove them after
            for (Element otherEmoji : otherEmojis) {
                NodeList keywordElems = otherEmoji.getElementsByTagName("keyword");

                for (int i = 0; i < keywordElems.getLength(); i++) {
                    keywords.add((Element) keywordElems.item(i));
                }

                rootElem.removeChild(otherEmoji);
            }

            //add keywords of the other emojis of the same value
            for (Element keyword : keywords) {
                newEmoji.appendChild(keyword);
            }

            rootElem.appendChild(newEmoji);
        }

        writeToFile(doc);
    }

    public void mergeDuplicateKeywords(Multimap<String, String> emojisWithDuplicateKeywords) {
        Document doc = emojiLoadingService.getDocument();

        List<Element> emojis = nodeListToElementList(doc.getElementsByTagName("emoji"));

        for (String emoji : emojisWithDuplicateKeywords.keys()) {
            //load the emoji element for the current emoji for which we want to merge duplicate keywords
            List<Element> emojiElems = emojis.stream().filter(e -> e.getAttribute("value").equals(emoji))
                    .collect(Collectors.toList());
            if (emojiElems.size() == 1) {
                Element emojiElem = emojiElems.get(0);
                List<Element> keywordElems = nodeListToElementList(emojiElem.getElementsByTagName("keyword"));

                //loop over the different pairs of duplicate keywords on current emoji
                for (String keyword : emojisWithDuplicateKeywords.get(emoji)) {
                    //load all duplicates for the current keyword
                    List<Element> selectedKeywords = keywordElems.stream().filter(k -> k.getTextContent().equals(keyword))
                            .collect(Collectors.toList());
                    //set replace = true for the new keyword if all keywords are true
                    Boolean isReplace = selectedKeywords.stream().allMatch(k -> Boolean.parseBoolean(k.getAttribute("replace")));

                    //remove duplicates
                    for (Element keywordElem : selectedKeywords) {
                        emojiElem.removeChild(keywordElem);
                    }

                    //create and add new keyword
                    Element newKeyword = doc.createElement("keyword");
                    newKeyword.setAttribute("replace", isReplace.toString());
                    newKeyword.setTextContent(keyword);
                    emojiElem.appendChild(newKeyword);
                }
            } else if (emojiElems.size() > 1) {
                throw new IllegalStateException("More than one emoji found for value " + emoji + ". Keyword cleaning failed.");
            } else {
                throw new IllegalStateException("No emoji found for value " + emoji + ". Keyword cleaning failed.");
            }
        }

        writeToFile(doc);
    }

    public void setKeywordsToLowerCase() {
        Document doc = emojiLoadingService.getDocument();
        List<Element> keywordElems = nodeListToElementList(doc.getElementsByTagName("keyword"));

        for (Element keywordElem : keywordElems) {
            String keyword = keywordElem.getTextContent();

            if (!keyword.equals(keyword.toLowerCase())) {
                keywordElem.setTextContent(keyword.toLowerCase());
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
        List<Element> elems = Lists.newArrayList();
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
        List<Element> elems = Lists.newArrayList();
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

    private List<Element> nodeListToElementList(NodeList nodeList) {
        List<Element> elements = Lists.newArrayList();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }

        return elements;
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
