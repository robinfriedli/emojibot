package api;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
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

    /**
     * @return all indices of StringList values that are words, meaning all characters are letters
     */
    List<Integer> getWordPositions();

    /**
     * find all indices of specified String within StringList
     *
     * @param s String to find
     * @return occurrences of said String
     */
    List<Integer> findPositionsOf(String s);

    /**
     * find all indices of specified String within StringList
     *
     * @param s String to find
     * @param ignoreCase define if you want to find matches regardless of capitalisation
     * @return occurrences of said String
     */
    List<Integer> findPositionsOf(String s, boolean ignoreCase);

    /**
     * checks if value at index is preceded by String s
     *
     * @param index of value
     * @param s String to check if precedes value at index
     */
    boolean valuePrecededBy(int index, String s);

    /**
     * checks if all values at the specified indices are preceded by String s
     *
     * @param indices of values to check
     * @param s String to check if precedes values at indices
     */
    boolean valuePrecededBy(List<Integer> indices, String s);

    /**
     * assert that any condition from one of the StringList methods is true
     *
     * e.g stringList.assertThat(p -> p.valuePrecededBy(3, "-"), "value at 3 not preceded by -")
     *
     * @param predicate  any StringList method that returns a boolean
     * @param errorMessage message to throw if assertion fails
     * @throws AssertionError if assertion fails
     */
    void assertThat(Predicate<StringList> predicate, String errorMessage) throws AssertionError;

    /**
     * assert that any condition from one of the StringList methods is true
     *
     * e.g stringList.assertThat(p -> p.valuePrecededBy(3, "-"))
     *
     * @param predicate  any StringList method that returns a boolean
     * @throws AssertionError if assertion fails
     */
    void assertThat(Predicate<StringList> predicate) throws AssertionError;
}
