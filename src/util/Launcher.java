package util;

import java.util.List;
import java.util.Scanner;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import core.Context;
import core.XmlManager;

public class Launcher {

    private static Context context = new Context();
    private static CommandHandler commandHandler = new CommandHandler(context);

    public static void main(String[] args) {
        XmlManager xmlManager = new XmlManager();
        if (xmlManager.getDocument() != null) {
            showMenu();
        } else {
            throw new IllegalStateException("Emoji loading failed");
        }
    }

    private static void showMenu() {
        System.out.println(buildMenu());
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();

        switch (input) {
            case "1":
                launchDiscordBot();
                break;

            case "2":
                transformText();
                break;

            case "3":
                addEmojis();
                break;

            case "4":
                removeEmojis();
                break;

            case "5":
                listEmojis();
                break;

            case "6":
                searchQuery();
                break;

            case "7":
                commandHandler.cleanXml(null);
                showMenu();
                break;

            case "8":
                return;

            default:
                throw new IllegalArgumentException("Illegal Argument");
        }
    }

    private static void launchDiscordBot() {
        commandHandler.cleanXml(null);
        DiscordListener discordListener = new DiscordListener(context, commandHandler);
        discordListener.launch();
    }

    private static void transformText() {
        System.out.println("Enter text to transform. Put text in quotation marks if you want to add arguments '-arg1 -arg2 \"text\"'");
        Scanner sc = new Scanner(System.in);
        commandHandler.transformText(sc.nextLine(), null, false);

        showMenu();
    }

    private static void addEmojis() {
        System.out.println("Type '\"emoji1, emoji2\" (optional: \"true, false\")' to add emojis.\n" +
            "Or type '\"emoji1, emoji2\" (optional: \"true, false\") \"keyword1, keyword2\" \"true false\"' to add emojis with keywords " +
            "or adjust replace value of existing keyword");
        System.out.print(System.lineSeparator());

        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        System.out.print(System.lineSeparator());
        commandHandler.saveEmojis(input, null, null);

        System.out.print(System.lineSeparator());
        showMenu();
    }

    private static void removeEmojis() {
        System.out.println("Type '\"emoji1, emoji2\"' to remove emojis " +
            "or '\"emoji1, emoji2\" \"keyword1, keyword2\"' to remove keywords from specified emojis");
        System.out.print(System.lineSeparator());

        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        System.out.print(System.lineSeparator());
        commandHandler.deleteEmojis(input, null, null);

        System.out.print(System.lineSeparator());
        showMenu();
    }

    private static void listEmojis() {
        List<Emoji> emojis = context.getUnicodeEmojis();
        List<DiscordEmoji> discordEmojis = context.getDiscordEmojis();
        StringBuilder builder = new StringBuilder();

        for (Emoji emoji : emojis) {
            builder.append(emoji.getEmojiValue()).append("\trandom: ").append(emoji.isRandom());

            List<Keyword> keywords = emoji.getKeywords();
            for (int i = 0; i < keywords.size(); i++) {
                if (i == 0) builder.append("\t");

                builder.append(keywords.get(i).getKeywordValue()).append(" (").append(keywords.get(i).isReplace()).append(")");

                if (i < keywords.size() - 1) builder.append(", ");
            }

            builder.append(System.lineSeparator());
        }

        if (!discordEmojis.isEmpty()) builder.append("Emojis from guilds:").append(System.lineSeparator());
        for (DiscordEmoji discordEmoji : discordEmojis) {
            builder.append(discordEmoji.getName()).append("\t").append(discordEmoji.getGuildName())
                .append("\trandom: ").append(discordEmoji.isRandom());

            List<Keyword> keywords = discordEmoji.getKeywords();
            for (int i = 0; i < keywords.size(); i++) {
                if (i == 0) builder.append("\t");

                builder.append(keywords.get(i).getKeywordValue()).append(" (").append(keywords.get(i).isReplace()).append(")");

                if (i < keywords.size() - 1) builder.append(", ");
            }

            builder.append(System.lineSeparator());
        }

        System.out.println(builder.toString());
        showMenu();
    }

    private static void searchQuery() {
        System.out.println("Enter search term:");
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        commandHandler.searchQuery(input, null);

        System.out.print(System.lineSeparator());
        showMenu();
    }

    private static String buildMenu() {
        return "1 - Launch Discord Bot" + System.lineSeparator() +
            "2 - Transform text to emojitext" + System.lineSeparator() +
            "3 - Add Emojis and Keywords or adjust replace value of existing Keyword" + System.lineSeparator() +
            "4 - Remove Emojis or Keywords" + System.lineSeparator() +
            "5 - List all Emojis and Keywords" + System.lineSeparator() +
            "6 - Search for Emojis and Keywords" + System.lineSeparator() +
            "7 - Clean up errors in XML configuration" + System.lineSeparator() +
            "8 - Exit";
    }
}
