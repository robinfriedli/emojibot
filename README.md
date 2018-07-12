# emojibot

Discord bot that adds emojis to text.
Uses the Java Discord API: https://github.com/DV8FromTheWorld/JDA

* Replaces all spaces with random emojis
* Replaces B with 🅱️
* Replaces keywords with emojis or adds emojis after keyword
    * One keyword can be applied to several emojis
* Supports custom guild emotes
* Allows you to use animated emotes without Discord Nitro
____________________________________________________________________________________________________________________

## Commands:

### Transform text into emojitext:

    Syntax:
    `e!e text` or `e!e -arg1 -arg2 "text"`

    Notice:
    arguments set property to the opposite of what's in the settings
    args: -rf (random formatting), -re (random emojis), -rb (replace b with 🅱️)

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
    or `e!add "emoji1, emoji2" "false, true"`

    Notice:
    The optional random flags at the end define whether the specified emoji will randomly be placed between words.
    In this case emoji1 will not and emoji2 will be randomly placed
    Default value is 'true'.

### Add emojis with keywords or adjust replace flag of existing keywords:

    Syntax:
    `e!add "emioji1, emoji2" "keyword1, keyword2" "true, false"`
    or `e!add "emoji1, emoji2" "false, true" "keyword1, keyword2" "true, false"`

    Notice:
    The optional first set of flags (true/false) defines whether the specified emoji will randomly be placed between words. Default is 'true'
    The mandatory second set of flags defines whether the keywords gets replaced (true) or the emoji gets placed after the keyword (false)
    In this instance keyword1 would get replaced and keyword2 wouldn't.
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

### Settings:

    Syntax:
    `e!settings`: show all settings
    `e!settings "REPLACE_B"`: show value of property
    `e!settings "REPLACE_B" "true"`: adjust value of property

____________________________________________________________________________________________________________________

## Setup:

Create a file called token.txt within the resources directory containing the token for your discord bot.
