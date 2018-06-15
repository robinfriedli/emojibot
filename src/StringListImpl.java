import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StringListImpl implements StringList {

    List<String> values;

    public StringListImpl(List<String> stringList) {
        this.values = stringList;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (String value : values) {
            builder.append(value);
        }

        return builder.toString();
    }

    @Override
    public String toSeparatedString() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            builder.append(values.get(i));

            if (i < values.size() - 1) {
                builder.append(", ");
            }
        }

        return builder.toString();
    }

    @Override
    public void replaceValueAt(int index, String value) {
        values.set(index, value);
    }

    public static StringList create(String string, String regex) {
        String[] stringList = string.split(regex);
        return create(stringList);
    }

    public static StringList create(List<String> stringList) {
        return new StringListImpl(stringList);
    }

    public static StringList create(String[] stringArray) {
        List<String> strings = new ArrayList<>(Arrays.asList(stringArray));

        return new StringListImpl(strings);
    }

    public static StringList charsToList(String string) {
        List<String> charsAsString = new ArrayList<>();
        for (Character character : string.toCharArray()) {
            charsAsString.add(character.toString());
        }

        return create(charsAsString);
    }
}
