package util;

import api.Emoji;
import api.Keyword;
import core.EmojiLoadingService;
import core.TextManipulationService;

import java.util.List;
import java.util.Scanner;

public class Launcher {

    private static CommandHandler commandHandler = new CommandHandler();

    public static void main(String[] args) {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        if (emojiLoadingService.getDocument() != null) {
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
        DiscordListener discordListener = new DiscordListener(commandHandler);
        discordListener.launch();
    }

    private static void transformText() {
        boolean randomFormat;

        System.out.println("Apply random formatting bold / italic (y/n)");
        Scanner sc = new Scanner(System.in);
        String argument = sc.nextLine();

        switch (argument) {
            case "y":
                randomFormat = true;
                break;

            case "n":
                randomFormat = false;
                break;

            default:
                throw new IllegalArgumentException("Invalid argument");
        }

        System.out.println("Enter text to transform:");
        Scanner inputSc = new Scanner(System.in);
        String input = inputSc.nextLine();

        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        TextManipulationService service = new TextManipulationService(randomFormat, emojiLoadingService.loadEmojis());
        System.out.println(service.getOutput(input));

        showMenu();
    }

    private static void addEmojis() {
        System.out.println("Type '\"emoji1, emoji2\"' to add emojis. " +
                "Or type '\"emoji1, emoji2\" \"keyword1, keyword2\" \"true false\"' to add emojis with keywords " +
                "or adjust replace value of existing keyword");
        System.out.print(System.lineSeparator());

        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();
        System.out.print(System.lineSeparator());
        commandHandler.saveEmojis(input, null);

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
        commandHandler.deleteEmojis(input, null);

        System.out.print(System.lineSeparator());
        showMenu();
    }

    private static void listEmojis() {
        EmojiLoadingService emojiLoadingService = new EmojiLoadingService();
        List<Emoji> emojis = emojiLoadingService.loadEmojis();
        StringBuilder builder = new StringBuilder();

        for (Emoji emoji : emojis) {
            builder.append(emoji.getEmojiValue());

            List<Keyword> keywords = emoji.getKeywords();
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
