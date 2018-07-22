package api;

import java.util.List;
import java.util.stream.Collectors;

public class DiscordEmoji extends Emoji {

    private String name;
    private String guildId;
    private String guildName;

    public DiscordEmoji(List<Keyword> keywords, String value, boolean random, String name, String guildId, String guildName) {
        super(keywords, value, random);
        this.name = name;
        this.guildId = guildId;
        this.guildName = guildName;
    }

    public DiscordEmoji(List<Keyword> keywords, String value, boolean random, State state, String name, String guildId, String guildName) {
        super(keywords, value, random, state);
        this.name = name;
        this.guildId = guildId;
        this.guildName = guildName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setGuildId(String guildId) {
        this.guildId = guildId;
    }

    public String getGuildId() {
        return guildId;
    }

    public void setGuildName(String guildName) {
        this.guildName = guildName;
    }

    public String getGuildName() {
        return guildName;
    }

    public static List<DiscordEmoji> getForGuild(List<DiscordEmoji> emojis, String guildId) {
        return emojis.stream().filter(e -> e.getGuildId().equals(guildId)).collect(Collectors.toList());
    }

    public static List<DiscordEmoji> getForKeyword(Keyword keyword, List<DiscordEmoji> emojis) {
        return emojis.stream().filter(e -> e.hasKeywordValue(keyword.getKeywordValue())).collect(Collectors.toList());
    }
}
