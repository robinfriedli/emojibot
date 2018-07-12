package api;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public interface StringList extends Iterable<String> {

    /**
     * @return size of list
     */
    int size();

    /**
     * @return value at index
     */
    String get(int i);

    /**
     * tries to return the value at the specified index and returns null instead of throwing and error
     * when out of bounds
     *
     * @param index of value
     * @return value at index or null
     */
    @Nullable
    String tryGet(int index);

    /**
     * @return true if StringList has no values
     */
    boolean isEmpty();

    /**
     * @param o any Object, false if not instance of String
     * @return true if StringList contains o
     */
    boolean contains(Object o);

    /**
     * Checks if StringList containes all Objects in List
     *
     * @param c Collection to check
     * @return true if StringList contains all elements
     */
    boolean containsAll(Collection c);

    /**
     * add String instance to StringList
     *
     * @param s String to add
     * @return true if added successfully
     */
    boolean add(String s);

    /**
     * adds all Strings of list to StringList
     *
     * @param strings to add
     * @return true if success
     */
    boolean addAll(List<String> strings);

    /**
     * Removes first occurrence of String from StringList
     *
     * @param s String to remove
     * @return true if modified
     */
    boolean remove(String s);

    /**
     * Remove all occurrences of String from StringList
     *
     * @param s String to remove
     * @return true if modified
     */
    boolean removeAll(String s);

    /**
     * Removes all elements of list from StringList
     *
     * @param strings List of elements to remove
     * @return true if success
     */
    boolean removeAll(List<String> strings);

    /**
     * Remove all values from StringList
     */
    void clear();

    /**
     * Removes al values from StringList except objects contained in List
     *
     * @param strings to keep
     * @return true on success
     */
    boolean retainAll(List<String> strings);

    /**
     * @return StringList values as array
     */
    String[] toArray();

    /**
     * @return StringList values as String
     */
    String toString();

    /**
     * @param separator String to separate StringList values
     * @return StringList values as String separated by passed String
     */
    String toSeparatedString(String separator);

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
    void set(int index, String value);

    /**
     * @return stream for StringList values
     */
    Stream<String> stream();


    /**
     * Retain values that only contain letters
     */
    StringList filterWords();

}
