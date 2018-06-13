import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class EmojiLoadingService {

    public static final Document DOC = getDocument();

    /**
     * loops over all emoji elements in the xml file
     * creates Emoji object with all of its keywords
     * adds Emojis to list
     *
     * @return Multimap
     */
    public static List<Emoji> loadEmojis() {
        NodeList emojiList = DOC.getElementsByTagName("emoji");
        List<Emoji> emojis = new ArrayList<>();

        for (int i = 0; i < emojiList.getLength(); i++) {
            Node emoji = emojiList.item(i);

            if (emoji.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) emoji;
                String emojiValue = elem.getAttribute("value");
                emojis.add(new Emoji(getKeywords(elem), emojiValue));
            }
        }

        return emojis;
    }

    private static List<Keyword> getKeywords(Element elem) {
        List<Keyword> keywordList = new ArrayList<>();

        NodeList keywords = elem.getElementsByTagName("keyword");
        for (int e = 0; e < keywords.getLength(); e++) {
            Element keyElem = (Element) keywords.item(e);
            String keyword = keyElem.getTextContent();
            boolean replace = Boolean.parseBoolean(keyElem.getAttribute("replace"));
            keywordList.add(new Keyword(keyword, replace));
        }

        return keywordList;
    }

    private static Document getDocument() {
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
