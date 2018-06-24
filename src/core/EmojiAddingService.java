package core;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.core.entities.MessageChannel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.AlertService;

import javax.annotation.Nullable;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EmojiAddingService {

    private EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
    private AlertService alertService = new AlertService();

    public void addEmojis(String[] emojis, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        List<String> addedEmojis = Lists.newArrayList();
        List<String> existingEmojis = Lists.newArrayList();

        for (String emoji : emojis) {
            if (!emojiExists(doc, emoji)) {
                Element emojiElem = doc.createElement("emoji");
                emojiElem.setAttribute("value", emoji);
                rootElem.appendChild(emojiElem);

                addedEmojis.add(emoji);
            } else {
                existingEmojis.add(emoji);
            }
        }

        writeToFile(doc);
        alertService.alertAddedEmojis(addedEmojis, existingEmojis, channel);
    }

    public void addEmojis(String[] emojis, String[] keywords, String[] replaceTags, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        List<String> addedEmojis = Lists.newArrayList();
        List<String> existingEmojis = Lists.newArrayList();
        Multimap<String, String> addedKeywords = HashMultimap.create();
        Multimap<String, String> existingKeywords = HashMultimap.create();
        Multimap<String, String> adjustedKeywords = HashMultimap.create();

        for (String emoji : emojis) {
            Element emojiElem;

            //only create new emoji if there isn't one with the same value, else load the existing emoji
            if (!emojiExists(doc, emoji)) {
                emojiElem = doc.createElement("emoji");
                emojiElem.setAttribute("value", emoji);

                addedEmojis.add(emoji);
            } else {
                emojiElem = getEmojiElem(doc, emoji);

                existingEmojis.add(emoji);
            }

            List<String> addedKeywordsForEmoji = Lists.newArrayList();
            List<String> adjustedKeywordsForEmoji = Lists.newArrayList();
            List<String> existingKeywordsForEmoji = Lists.newArrayList();
            for (int i = 0; i < keywords.length; i++) {
                Element keywordElem;

                //only create new keyword if it doesn't already exist on the same emoji, else load existing keyword
                // and adjust replace flag
                if (!keywordExists(emojiElem, keywords[i])) {
                    keywordElem = doc.createElement("keyword");
                    keywordElem.setAttribute("replace", replaceTags[i]);
                    keywordElem.setTextContent(keywords[i]);

                    addedKeywordsForEmoji.add(keywords[i]);
                } else {
                    keywordElem = getKeywordElem(emojiElem, keywords[i]);

                    if (!keywordElem.getAttribute("replace").equals(replaceTags[i])) {
                        keywordElem.setAttribute("replace", replaceTags[i]);

                        adjustedKeywordsForEmoji.add(keywords[i]);
                    } else {
                        existingKeywordsForEmoji.add(keywords[i]);
                    }
                }

                emojiElem.appendChild(keywordElem);
            }
            addedKeywordsForEmoji.forEach(k -> addedKeywords.put(emoji, k));
            adjustedKeywordsForEmoji.forEach(k -> adjustedKeywords.put(emoji, k));
            existingKeywordsForEmoji.forEach(k -> existingKeywords.put(emoji, k));

            rootElem.appendChild(emojiElem);
        }

        writeToFile(doc);
        alertService.alertAddedEmojis(addedEmojis, existingEmojis, addedKeywords, adjustedKeywords, existingKeywords, channel);
    }

    public void removeEmojis(List<String> emojisToRemove, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();
        List<Element> emojiElems = getEmojiElems(doc, emojisToRemove);
        List<String> removedEmojis = Lists.newArrayList();

        for (Element emoji : emojiElems) {
            String emojiValue = emoji.getAttribute("value");

            if (emojisToRemove.contains(emojiValue)) {
                rootElem.removeChild(emoji);

                emojisToRemove.remove(emojiValue);
                removedEmojis.add(emojiValue);
            }
        }

        writeToFile(doc);
        alertService.alertRemovedEmojis(removedEmojis, emojisToRemove, channel);
    }

    public void removeKeywords(List<String> emojis, List<String> keywordsToRemove, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        List<Element> emojiElems = getEmojiElems(doc, emojis);
        Multimap<String, String> removedKeywords = HashMultimap.create();
        Multimap<String, String> missingKeywords = HashMultimap.create();

        for (Element emoji : emojiElems) {
            String emojiValue = emoji.getAttribute("value");

            if (emojis.contains(emojiValue)) {
                List<Element> keywordElems = getKeywordElems(emoji, keywordsToRemove);

                List<String> keywordsOnEmoji = Lists.newArrayList(keywordsToRemove);
                for (Element keyword : keywordElems) {
                    String keywordValue = keyword.getTextContent();

                    if (keywordsToRemove.contains(keywordValue)) {
                        emoji.removeChild(keyword);

                        keywordsOnEmoji.remove(keywordValue);
                        removedKeywords.put(emojiValue, keywordValue);
                    }
                }

                //if keywordsOnEmoji is not empty that means not all selected keywords have been found on this emoji
                if (!keywordsOnEmoji.isEmpty()) {
                    keywordsOnEmoji.forEach(k -> missingKeywords.put(emojiValue, k));
                }
            }

            emojis.remove(emojiValue);
        }

        writeToFile(doc);
        alertService.alertRemovedKeywords(emojis, removedKeywords, missingKeywords, channel);
    }

    public void mergeDuplicateEmojis(Set<String> duplicateEmojis, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        Element rootElem = doc.getDocumentElement();

        NodeList emojis = doc.getElementsByTagName("emoji");
        List<Element> newEmojis = Lists.newArrayList();
        List<String> mergedEmojis = Lists.newArrayList();

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
            mergedEmojis.add(emojiValue);
        }

        writeToFile(doc);
        alertService.alertMergedEmojis(mergedEmojis, channel);
    }

    public void mergeDuplicateKeywords(Multimap<String, String> emojisWithDuplicateKeywords, @Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();

        List<Element> emojis = nodeListToElementList(doc.getElementsByTagName("emoji"));
        Multimap<String, String> mergedKeywords = HashMultimap.create();

        for (String emoji : emojisWithDuplicateKeywords.keys()) {
            //load the emoji element for the current emoji for which we want to merge duplicate keywords
            List<Element> emojiElems = emojis.stream().filter(e -> e.getAttribute("value").equals(emoji))
                    .collect(Collectors.toList());
            if (emojiElems.size() == 1) {
                Element emojiElem = emojiElems.get(0);
                List<Element> keywordElems = nodeListToElementList(emojiElem.getElementsByTagName("keyword"));

                //loop over the different duplicate keyword values on current emoji
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

                    mergedKeywords.put(emoji, keyword);
                }
            } else if (emojiElems.size() > 1) {
                throw new IllegalStateException("More than one emoji found for value " + emoji + ". Keyword cleaning failed.");
            } else {
                throw new IllegalStateException("No emoji found for value " + emoji + ". Keyword cleaning failed.");
            }
        }

        writeToFile(doc);
        alertService.alertMergedKeywords(mergedKeywords, channel);
    }

    public void setKeywordsToLowerCase(@Nullable MessageChannel channel) {
        Document doc = emojiLoadingService.getDocument();
        List<Element> keywordElems = nodeListToElementList(doc.getElementsByTagName("keyword"));
        List<String> changedKeywords = Lists.newArrayList();

        for (Element keywordElem : keywordElems) {
            String keyword = keywordElem.getTextContent();

            if (!keyword.equals(keyword.toLowerCase())) {
                keywordElem.setTextContent(keyword.toLowerCase());

                changedKeywords.add(keyword);
            }
        }

        writeToFile(doc);
        alertService.alertUpperCaseKeywords(changedKeywords, channel);
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
