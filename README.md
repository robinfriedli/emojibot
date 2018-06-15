# emojibot

Discord bot that adds emojis to text.
Uses the Java Discord API: https://github.com/DV8FromTheWorld/JDA

* Replaces all spaces with random emojis
* Replaces B with 🅱️
* Replaces keywords with emojis or adds emojis after keyword
    * One keyword can be applied to several emojis

____________________________________________________________________________________________________________________

## Commands:

### Transform text into emojitext:

    Syntax:
    `e!e text`

### List all saved emojis:

    Syntax:
    `e!list`

### Search for saved emojis and keywords:

    Syntax:
    `e!search "searchTerm"`

    Notice:
    Shows found emoji and lists its keyword or shown found keyword and lists what emojis it occurs on

### Add emojis:

    Syntax:
    `e!add "emoji1, emoji2"`

### Add emojis with keywords or adjust replace flag of existing keywords:

    Syntax:
    `e!add "emioji1, emoji2" "keyword1, keyword2" "true, false"`

    Notice:
    true or false defines whether the keywords gets replaced (true) or
    the emoji gets placed after the keyword (false)
    in this instance keyword1 would get replaced and keyword2 wouldn't.
    All keywords will be applied to all emojis.
    Keywords MUST be lower case.

### Remove emojis:

    Syntax:
    `e!rm "emoji1, emoji2"`

### Remove keywords:

    Syntax:
    `e!rm "emoji1, emoji2" "keyword1, keyword2"`

    Notice:
    Specify the emojis from which you want to remove the keywords.

### Merge duplicate emojis and duplicate keywords on the same emoji:

    Syntax:
    `e!clean`

    Notice: emojis and keywords can only duplicate if manually meddling with the xml file,
    the bot does not duplicate anything
    when merging keywords the replace flag is set to true if all keywords are true

____________________________________________________________________________________________________________________

## Setup:

Create a file called token.txt within the resources directory containing the token for your discord bot.

Download the JDA jar from https://github.com/DV8FromTheWorld/JDA

Also requires the google guava library
