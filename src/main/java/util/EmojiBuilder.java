package util;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import net.robinfriedli.jxp.persist.Context;

import java.util.List;

public class EmojiBuilder {

    private List<KeywordBuilder> keywords;
    private String value;
    private boolean random = true;

    private String name;
    private String guildId;
    private String guildName;

    public EmojiBuilder addKeywords(KeywordBuilder... keywords) {
        if (this.keywords == null) {
            this.keywords = Lists.newArrayList(keywords);
        } else {
            this.keywords.addAll(Lists.newArrayList(keywords));
        }

        return this;
    }

    public EmojiBuilder addKeywords(List<KeywordBuilder> keywords) {
        if (this.keywords == null) {
            this.keywords = Lists.newArrayList(keywords);
        } else {
            this.keywords.addAll(Lists.newArrayList(keywords));
        }

        return this;
    }

    public EmojiBuilder setValue(String value) {
        this.value = value;
        return this;
    }

    public EmojiBuilder setRandom(boolean random) {
        this.random = random;
        return this;
    }

    public EmojiBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public EmojiBuilder setGuildId(String guildId) {
        this.guildId = guildId;
        return this;
    }

    public EmojiBuilder setGuildName(String guildName) {
        this.guildName = guildName;
        return this;
    }

    public Emoji createEmoji(Context context) {
        assert value != null;

        Emoji existingEmoji = context.getElement(value, Emoji.class);
        if (existingEmoji != null) {
            return castEmoji(existingEmoji, context);
        } else {
            List<Keyword> keywords = Lists.newArrayList();
            if (this.keywords != null && !this.keywords.isEmpty()) {
                for (KeywordBuilder keyword : this.keywords) {
                    keywords.add(keyword.createKeyword(context));
                }
            }
            Emoji emoji = new Emoji(keywords, value, random, context);
            emoji.persist();
            return emoji;
        }
    }

    public DiscordEmoji createDiscordEmoji(Context context) {
        assert value != null;
        assert name != null;
        assert guildId != null;
        assert guildName != null;

        DiscordEmoji existingEmoji = context.getElement(value, DiscordEmoji.class);
        if (existingEmoji != null) {
            return castEmoji(existingEmoji, context);
        } else {
            List<Keyword> keywords = Lists.newArrayList();
            if (this.keywords != null && !this.keywords.isEmpty()) {
                for (KeywordBuilder keyword : this.keywords) {
                    keywords.add(keyword.createKeyword(context));
                }
            }
            DiscordEmoji discordEmoji = new DiscordEmoji(keywords, value, random, name, guildId, guildName, context);
            discordEmoji.persist();
            return discordEmoji;
        }
    }

    private <E extends Emoji> E castEmoji(E existingEmoji, Context context) {
        if (existingEmoji.isRandom() != random) {
            existingEmoji.setRandom(random);
        }

        if (keywords != null && !keywords.isEmpty()) {
            for (KeywordBuilder keyword : keywords) {
                Keyword existingKeyword = existingEmoji.getKeyword(keyword.getKeywordValue());
                if (existingKeyword == null) {
                    existingEmoji.addKeyword(keyword.createKeyword(context));
                } else if (existingKeyword.isReplace() != keyword.isReplace()) {
                    existingKeyword.setReplace(keyword.isReplace());
                }
            }
        }

        return existingEmoji;
    }
}
