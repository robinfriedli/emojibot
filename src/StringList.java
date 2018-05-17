import java.util.List;

public interface StringList {

    /**
     * @return size of list
     */
    int size();

    /**
     * @return StringList values as String
     */
    String toString();

    /**
     * @return StringList values as List
     */
    List<String> getValues();

    /**
     * replaces value of element in StringList
     *
     * @param index
     * @param value
     */
    void replaceValueAt(int index, String value);
}
