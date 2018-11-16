package api;

import com.google.common.collect.Lists;
import net.robinfriedli.jxp.api.AbstractXmlElement;
import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Keyword extends AbstractXmlElement {

    public Keyword(String keywordValue, boolean replace, Context context) {
        super("keyword", buildAttributes(replace), keywordValue, context);
    }

    public Keyword(Element element, Context context) {
        super(element, context);
    }

    private static Map<String, String> buildAttributes(boolean replace) {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("replace", Boolean.toString(replace));
        return attributeMap;
    }

    @Override
    public String getId() {
        return getTextContent();
    }

    public String getKeywordValue() {
        return getTextContent();
    }

    public void setKeywordValue(String keyword) {
        setTextContent(keyword);
    }

    public boolean isReplace() {
        return Boolean.parseBoolean(getAttribute("replace").getValue());
    }

    public void setReplace(boolean replace) {
        setAttribute("replace", Boolean.toString(replace));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Keyword)) return false;
        return this.getKeywordValue().equals(((Keyword) o).getKeywordValue())
            && isReplace() == ((Keyword) o).isReplace();
    }

    public static List<String> getAllKeywordValues(List<Keyword> keywords) {
        List<String> keywordValues = Lists.newArrayList();
        for (Keyword keyword : keywords) {
            keywordValues.add(keyword.getKeywordValue());
        }
        return keywordValues;
    }

    public static List<Keyword> getSelectedKeywords(List<Keyword> keywords, String value) {
        List<Keyword> selectedKeywords = Lists.newArrayList();
        for (Keyword keyword : keywords) {
            if (keyword.getKeywordValue().equals(value)) {
                selectedKeywords.add(keyword);
            }
        }

        if (!selectedKeywords.isEmpty()) {
            return selectedKeywords;
        } else {
            throw new IllegalStateException("No keywords found for value " + value + " within provided list");
        }
    }

}
