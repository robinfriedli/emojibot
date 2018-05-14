import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
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

    public static Multimap<String, String> loadKeywords() {
        Document doc = getDocument();
        Multimap<String, String> emojiMap = HashMultimap.create();
        NodeList emojiList = doc.getElementsByTagName("emoji");

        for (int i = 0; i < emojiList.getLength(); i++) {
            Node emoji = emojiList.item(i);

            if (emoji.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) emoji;
                String emojiValue = elem.getAttribute("value");
                for (String keyword : getKeywords(elem)) {
                    emojiMap.put(keyword, emojiValue);
                }

            }
        }

        return emojiMap;
    }

    private static List<String> getKeywords(Element elem) {
        List<String> keywordList = new ArrayList<>();

        NodeList keywords = elem.getElementsByTagName("keyword");
        for(int e = 0; e < keywords.getLength(); e++) {
            Element keyElem = (Element) keywords.item(e);
            keywordList.add(keyElem.getTextContent());
        }

        return keywordList;
    }

    public static Document getDocument() {
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

    public static List<String> getEmojiStrings() {
        List<String> emojis = Lists.newArrayList();

        emojis.addAll(loadKeywords().values());

        return emojis;
    }
}
