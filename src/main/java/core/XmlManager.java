package core;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import util.DiscordListener;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
@Deprecated // replaced by XmlPersister in new Persistence API
public class XmlManager {

    private Document doc = getDocument();

    public <E extends Emoji> void addEmojiElem(E emoji) {
        Element rootElem = doc.getDocumentElement();
        if (emoji instanceof DiscordEmoji) {
            Element emojiElem = doc.createElement("discord-emoji");
            emojiElem.setAttribute("value", emoji.getEmojiValue());
            emojiElem.setAttribute("random", Boolean.toString(emoji.isRandom()));
            emojiElem.setAttribute("name", ((DiscordEmoji) emoji).getName());
            emojiElem.setAttribute("guildId", ((DiscordEmoji) emoji).getGuildId());
            emojiElem.setAttribute("guildName", ((DiscordEmoji) emoji).getGuildName());

            for (Keyword keyword : emoji.getKeywords()) {
                Element keywordElem = doc.createElement("keyword");
                keywordElem.setAttribute("replace", Boolean.toString(keyword.isReplace()));
                keywordElem.setTextContent(keyword.getKeywordValue());
                emojiElem.appendChild(keywordElem);
            }

            rootElem.appendChild(emojiElem);
        } else {
            Element emojiElem = doc.createElement("emoji");
            emojiElem.setAttribute("value", emoji.getEmojiValue());
            emojiElem.setAttribute("random", Boolean.toString(emoji.isRandom()));

            for (Keyword keyword : emoji.getKeywords()) {
                Element keywordElem = doc.createElement("keyword");
                keywordElem.setAttribute("replace", Boolean.toString(keyword.isReplace()));
                keywordElem.setTextContent(keyword.getKeywordValue());
                emojiElem.appendChild(keywordElem);
            }

            rootElem.appendChild(emojiElem);
        }
    }

    public <E extends Emoji> void removeEmojiElem(E emoji) {
        Element rootElem = doc.getDocumentElement();
        String tagName = emoji instanceof DiscordEmoji ? "discord-emoji" : "emoji";
        try {
            rootElem.removeChild(requireEmojiElem(tagName, emoji.getEmojiValue()));
        } catch (IllegalStateException e) {
            System.out.println("Duplicate emoji detected: " + emoji.getEmojiValue());
            // remove all duplicated
            List<Element> emojiElems = nodeListToElementList(doc.getElementsByTagName(tagName));
            emojiElems.stream()
                .filter(elem -> elem.getAttribute("value").equals(emoji.getEmojiValue()))
                .forEach(rootElem::removeChild);
        }
    }

    public <E extends Emoji> void adjustEmojiElem(E emoji, boolean random) {
        String tagName = emoji instanceof DiscordEmoji ? "discord-emoji" : "emoji";
        Element emojiElem = requireEmojiElem(tagName, emoji.getEmojiValue());
        emojiElem.setAttribute("random", Boolean.toString(random));
    }

    public <E extends Emoji> void addKeywords(E emoji, List<Keyword> keywords) {
        if (emoji instanceof DiscordEmoji) {
            Element emojiElem = requireEmojiElem("discord-emoji", emoji.getEmojiValue());

            for (Keyword keyword : keywords) {
                Element keywordElem = doc.createElement("keyword");
                keywordElem.setTextContent(keyword.getKeywordValue());
                keywordElem.setAttribute("replace", Boolean.toString(keyword.isReplace()));

                emojiElem.appendChild(keywordElem);
            }
        } else {
            Element emojiElem = requireEmojiElem("emoji", emoji.getEmojiValue());

            for (Keyword keyword : keywords) {
                Element keywordElem = doc.createElement("keyword");
                keywordElem.setTextContent(keyword.getKeywordValue());
                keywordElem.setAttribute("replace", Boolean.toString(keyword.isReplace()));

                emojiElem.appendChild(keywordElem);
            }
        }
    }

    public <E extends Emoji> void removeKeywords(E emoji, List<Keyword> keywords) {
        String tagName = emoji instanceof DiscordEmoji ? "discord-emoji" : "emoji";
        Element emojiElem = requireEmojiElem(tagName, emoji.getEmojiValue());

        for (Keyword keyword : keywords) {
            Element keywordElem = requireKeywordElem(emojiElem, keyword.getKeywordValue());
            emojiElem.removeChild(keywordElem);
        }
    }

    /**
     * used to delete keywords with duplicates
     *
     * @param emoji to delete keywords from
     * @param keywords with duplicates
     * @param <E> Emoji or a subclass e.g. DiscordEmoji
     */
    public <E extends Emoji> void removeAllKeywords(E emoji, List<Keyword> keywords) {
        String tagName = emoji instanceof DiscordEmoji ? "discord-emoji" : "emoji";
        Element emojiElem = requireEmojiElem(tagName, emoji.getEmojiValue());

        for (Keyword keyword : keywords) {
            List<Element> keywordElems = nodeListToElementList(emojiElem.getElementsByTagName("keyword"));
            keywordElems.stream()
                .filter(k -> k.getTextContent().equals(keyword.getKeywordValue()))
                .forEach(emojiElem::removeChild);
        }
    }
/*
    public <E extends Emoji> void adjustKeywords(E emoji, List<KeywordChangingEvent> changedKeywords) {
        String tagName = emoji instanceof DiscordEmoji ? "discord-emoji" : "emoji";
        Element emojiElem = requireEmojiElem(tagName, emoji.getEmojiValue());

        for (KeywordChangingEvent keywordChangingEvent : changedKeywords) {
            String oldKeywordValue = keywordChangingEvent.getOldValue();
            Keyword adjustedKeyword = keywordChangingEvent.getAdjustedKeyword();

            try {
                Element keywordElem = requireKeywordElem(emojiElem, oldKeywordValue);
                keywordElem.setAttribute("replace", Boolean.toString(adjustedKeyword.isReplace()));
                keywordElem.setTextContent(adjustedKeyword.getKeywordValue());
            } catch (IllegalStateException e) {
                System.out.println("Duplicate Keyword detected: " + oldKeywordValue);
                nodeListToElementList(emojiElem.getElementsByTagName("keyword")).stream()
                    .filter(k -> k.getTextContent().equals(oldKeywordValue))
                    .forEach(k -> k.setTextContent(adjustedKeyword.getKeywordValue()));
            }
        }
    }
*/
    public List<Element> getEmojiElems() {
        return nodeListToElementList(doc.getElementsByTagName("emoji"));
    }

    public List<Element> getDiscordEmojiElems() {
        return nodeListToElementList(doc.getElementsByTagName("discord-emoji"));
    }

    public List<Element> getKeywordElems(Element emojiElem) {
        return nodeListToElementList(emojiElem.getElementsByTagName("keyword"));
    }

    public void writeToFile() {
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

    private Element requireEmojiElem(String tagName, String value) {
        List<Element> emojiElems = nodeListToElementList(doc.getElementsByTagName(tagName));
        List<Element> foundEmojis = emojiElems.stream()
            .filter(e -> e.getAttribute("value").equals(value))
            .collect(Collectors.toList());

        if (foundEmojis.size() == 1) {
            return foundEmojis.get(0);
        } else if (foundEmojis.isEmpty()) {
            throw new IllegalStateException("No emoji found with value " + value);
        } else {
            throw new IllegalStateException("Duplicate emojis found for value " + value + ". Try " + DiscordListener.COMMAND_CLEAN);
        }
    }

    private Element requireKeywordElem(Element emojiElem, String value) {
        List<Element> foundKeywords = nodeListToElementList(emojiElem.getElementsByTagName("keyword")).stream()
            .filter(elem -> elem.getTextContent().equals(value))
            .collect(Collectors.toList());

        if (foundKeywords.size() == 1) {
            return foundKeywords.get(0);
        } else if (foundKeywords.size() > 1) {
            throw new IllegalStateException("More than one keyword found for value " + value + " on emoji " + emojiElem.getAttribute("value")
                + ". Try " + DiscordListener.COMMAND_CLEAN);
        } else {
            throw new IllegalStateException("No keyword found for value " + value + " on emoji " + emojiElem.getAttribute("value"));
        }
    }

    private List<Element> nodeListToElementList(NodeList nodeList) {
        List<Element> elements = Lists.newArrayList();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elements.add((Element) nodeList.item(i));
        }

        return elements;
    }

    public Document getDocument() {
        try {
            File xml = new File("./resources/emojis.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(xml);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
