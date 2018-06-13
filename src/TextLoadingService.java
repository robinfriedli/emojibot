import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class TextLoadingService {

    public static String loadToken() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./resources/token.txt"));
            String token = reader.readLine();
            reader.close();

            return token;
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Token loading failed");
    }

    public static String loadHelp() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./resources/help.txt"));
            StringBuilder builder = new StringBuilder();
            String line = reader.readLine();

            while (line != null) {
                builder.append(line);
                builder.append(System.lineSeparator());

                line = reader.readLine();
            }

            reader.close();
            return builder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Help loading failed");
    }

}
