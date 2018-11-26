package api;

import net.robinfriedli.jxp.api.XmlElement;
import net.robinfriedli.jxp.persist.Context;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscordEmoji extends Emoji {

    public DiscordEmoji(List<XmlElement> keywords,
                        String value,
                        boolean random,
                        String name,
                        String guildId,
                        String guildName,
                        Context context) {
        super("discord-emoji", keywords, buildAttributes(value, random, name, guildId, guildName), context);
    }

    public DiscordEmoji(Element element, Context context) {
        super(element, context);
    }

    public DiscordEmoji(Element element,
                        List<XmlElement> keywords,
                        Context context) {
        super(element, keywords, context);
    }

    private static Map<String, String> buildAttributes(String value, boolean random, String name, String guildId, String guildName) {
        Map<String, String> attributeMap = new HashMap<>();
        attributeMap.put("value", value);
        attributeMap.put("random", Boolean.toString(random));
        attributeMap.put("name", name);
        attributeMap.put("guildId", guildId);
        attributeMap.put("guildName", guildName);
        return attributeMap;
    }

    public void setName(String name) {
        setAttribute("name", name);
    }

    public String getName() {
        return getAttribute("name").getValue();
    }

    public void setGuildId(String guildId) {
        setAttribute("guildId", guildId);
    }

    public String getGuildId() {
        return getAttribute("guildId").getValue();
    }

    public void setGuildName(String guildName) {
        setAttribute("guildName", guildName);
    }

    public String getGuildName() {
        return getAttribute("guildName").getValue();
    }

    public static List<DiscordEmoji> getForGuild(List<DiscordEmoji> emojis, String guildId) {
        return emojis.stream().filter(e -> e.getGuildId().equals(guildId)).collect(Collectors.toList());
    }

    public static List<DiscordEmoji> getForKeyword(Keyword keyword, List<DiscordEmoji> emojis) {
        return emojis.stream().filter(e -> e.hasKeywordValue(keyword.getKeywordValue())).collect(Collectors.toList());
    }
}
