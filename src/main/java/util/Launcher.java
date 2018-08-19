package util;

import api.DiscordEmoji;
import api.Emoji;
import api.Keyword;
import com.google.common.collect.Lists;
import core.ContextManager;
import core.PersistenceManager;

import java.util.List;
import java.util.Scanner;

public class Launcher {

    private static ContextManager contextManager = new ContextManager(
        "./resources/emojis.xml", new PersistenceManager(), Lists.newArrayList(new AlertEventListener(new AlertService())));
    private static CommandHandler commandHandler = new CommandHandler(contextManager.getContext());

    public static void main(String[] args) {
        if (contextManager.getContext().getPersistenceManager().getXmlPersister().getDocument() != null) {
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
        DiscordListener discordListener = new DiscordListener(contextManager, commandHandler);

        System.out.println("Select Mode:");
        System.out.println("1 - SHARED (all guilds will share the same emojis, recommended if you want to share guild emotes)");
        System.out.println("2 - PARTITIONED (guilds will be completely separated, recommended if one emojibot instance " +
            "is shared between guilds that don't have anything to do with each other)");
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();

        switch (input) {
            case "1":
                discordListener.launch(DiscordListener.Mode.SHARED);
                break;
            case "2":
                discordListener.launch(DiscordListener.Mode.PARTITIONED);
                break;
            default:
                throw new IllegalArgumentException();
        }
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
        List<Emoji> emojis = contextManager.getContext().getInstancesOf(Emoji.class, DiscordEmoji.class);
        List<DiscordEmoji> discordEmojis = contextManager.getContext().getInstancesOf(DiscordEmoji.class);
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
