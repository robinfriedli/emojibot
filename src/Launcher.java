import java.util.Scanner;

public class Launcher {

    public static void main(String[] args) {
        if (EmojiLoadingService.DOC != null) {
            boolean randFormat;

            System.out.println("Should the text be randomly formatted italic / bold etc (WhatsApp)? y/n");
            Scanner decisionSc = new Scanner(System.in);
            String decision = decisionSc.nextLine();

            switch (decision) {
                case "y":
                    randFormat = true;
                    break;

                case "n":
                    randFormat = false;
                    break;

                default:
                    throw new IllegalArgumentException("Invalid option, dumbass");

            }

            TextManipulationService service = new TextManipulationService(randFormat, EmojiLoadingService.loadEmojis());
            Scanner inputSc = new Scanner(System.in);
            String output = service.getOutput(inputSc.nextLine());
            System.out.println(output);
        } else {
            throw new IllegalStateException("Emoji loading failed");
        }
    }
}
