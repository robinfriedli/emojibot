package core;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.List;

@Deprecated // deprecated as of v2.3, use PersistenceManager instead
public class EmojiLoadingService {

    /**
     * loops over all emoji elements in the xml file
     * creates Emoji object with all of its keywords
     * adds Emojis to list
     *
     * @return Multimap
     */
    @Deprecated
    public List<Emoji> loadEmojis() {
        Document doc = getDocument();
        NodeList emojiList = doc.getElementsByTagName("emoji");
        List<Emoji> emojis = Lists.newArrayList();

        for (int i = 0; i < emojiList.getLength(); i++) {
            Node emoji = emojiList.item(i);

            if (emoji.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) emoji;
                String emojiValue = elem.getAttribute("value");
                String randomValue = elem.getAttribute("random");
                boolean random = randomValue.equals("") || Boolean.parseBoolean(randomValue);
                emojis.add(new Emoji(getKeywords(elem), emojiValue, random));
            }
        }

        return emojis;
    }

    @Deprecated
    public List<DiscordEmoji> loadDiscordEmojis() {
        Document doc = getDocument();
        NodeList emojiList = doc.getElementsByTagName("discord-emoji");
        List<DiscordEmoji> emojis = Lists.newArrayList();

        for (int i = 0; i < emojiList.getLength(); i++) {
            Node emoji = emojiList.item(i);

            if (emoji.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) emoji;
                String name = elem.getAttribute("name");
                String guildId = elem.getAttribute("guildId");
                String guildName = elem.getAttribute("guildName");
                String emojiValue = elem.getAttribute("value");
                String randomValue = elem.getAttribute("random");
                boolean random = randomValue.equals("") || Boolean.parseBoolean(randomValue);
                emojis.add(new DiscordEmoji(getKeywords(elem), emojiValue, random, name, guildId, guildName));
            }
        }

        return emojis;
    }

    private List<Keyword> getKeywords(Element elem) {
        List<Keyword> keywordList = Lists.newArrayList();

        NodeList keywords = elem.getElementsByTagName("keyword");
        for (int e = 0; e < keywords.getLength(); e++) {
            Element keyElem = (Element) keywords.item(e);
            String keyword = keyElem.getTextContent();
            boolean replace = Boolean.parseBoolean(keyElem.getAttribute("replace"));
            keywordList.add(new Keyword(keyword, replace));
        }

        return keywordList;
    }

    @Deprecated
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
