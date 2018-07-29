package api;

import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.common.collect.Lists;

public class StringListImpl implements StringList {

    private List<String> values;

    public StringListImpl(List<String> stringList) {
        this.values = stringList;
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public String get(int i) {
        return values.get(i);
    }

    @Override
    public String tryGet(int i) {
        if (i < size() && i >= 0) {
            return get(i);
        }
        return null;
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof String) {
            return values.contains(o);
        } else {
            return false;
        }
    }

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return values.iterator();
    }

    @Override
    public String[] toArray() {
        String[] array = new String[size()];

        for (int i = 0; i < size(); i++) {
            array[i] = get(i);
        }

        return array;
    }

    @Override
    public boolean add(String s) {
        return values.add(s);
    }

    @Override
    public boolean addAll(List<String> strings) {
        return values.addAll(strings);
    }

    @Override
    public boolean remove(String s) {
        return values.remove(s);
    }

    @Override
    public boolean removeAll(String s) {
        boolean modified = false;

        while (values.contains(s)) {
            values.remove(s);
            modified = true;
        }

        return modified;
    }

    @Override
    public boolean removeAll(List<String> strings) {
        return values.removeAll(strings);
    }

    @Override
    public void clear() {
        values.clear();
    }

    @Override
    public boolean retainAll(List<String> strings) {
        return values.retainAll(strings);
    }

    @Override
    public boolean containsAll(Collection c) {
        return values.containsAll(c);
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
    public String toSeparatedString(String separator) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < values.size(); i++) {
            builder.append(values.get(i));

            if (i < values.size() - 1) {
                builder.append(separator);
            }
        }

        return builder.toString();
    }

    @Override
    public void set(int index, String value) {
        values.set(index, value);
    }

    @Override
    public StringList filterWords() {
        StringList stringList = create();
        for (String value : values) {
            Character[] chars = stringToCharacterArray(value);
            if (Arrays.stream(chars).allMatch(Character::isLetter)) {
                stringList.add(value);
            }
        }

        return stringList;
    }

    @Override
    public List<Integer> getWordPositions() {
        List<Integer> positions = Lists.newArrayList();

        for (int i = 0; i < this.size(); i++) {
            String value = get(i);
            Character[] chars = stringToCharacterArray(value);

            if (Arrays.stream(chars).allMatch(Character::isLetter)) {
                positions.add(i);
            }
        }

        return positions;
    }

    @Override
    public List<Integer> findPositionsOf(String s) {
        return findPositionsOf(s, false);
    }

    @Override
    public List<Integer> findPositionsOf(String s, boolean ignoreCase) {
        List<Integer> positions = Lists.newArrayList();

        for (int i = 0; i < size(); i++) {
            String value = get(i);

            if (ignoreCase ? s.equalsIgnoreCase(value) : s.equals(value)) {
                positions.add(i);
            }
        }

        return positions;
    }

    @Override
    public boolean valuePrecededBy(int i, String s) {
        if (i == 0) return false;
        return get(i - 1).equals(s);
    }

    @Override
    public boolean valuePrecededBy(List<Integer> indices, String s) {
        return indices.stream().allMatch(i -> valuePrecededBy(i, s));
    }

    @Override
    public void assertThat(Predicate<StringList> predicate, String errorMessage) throws AssertionError {
        if (!predicate.test(this)) {
            throw new AssertionError(errorMessage);
        }
    }

    @Override
    public void assertThat(Predicate<StringList> predicate) throws AssertionError {
        if (!predicate.test(this)) {
            throw new AssertionError();
        }
    }

    public static StringList create(String string, String regex) {
        String[] stringList = string.split(regex);
        return create(stringList);
    }

    public static StringList separateString(String string, String regex) {
        String[] strings = string.split(regex);
        StringList stringList = new StringListImpl(Lists.newArrayList());

        for (int i = 0; i < strings.length; i++) {
            stringList.add(strings[i]);
            if (i < strings.length - 1) stringList.add(regex);
        }

        return stringList;
    }

    public static StringList createSentences(String input) {
        List<String> sentences = Lists.newArrayList();

        BreakIterator iterator = BreakIterator.getSentenceInstance();
        iterator.setText(input);
        int start = iterator.first();

        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            sentences.add(input.substring(start, end));
        }

        return create(sentences);
    }

    public static StringList createWords(String input) {
        List<String> words = Lists.newArrayList();

        BreakIterator iterator = BreakIterator.getWordInstance();
        iterator.setText(input);
        int start = iterator.first();

        for (int end = iterator.next(); end != BreakIterator.DONE; start = end, end = iterator.next()) {
            words.add(input.substring(start, end));
        }

        return create(words);
    }

    public static StringList create(List<String> stringList) {
        return new StringListImpl(stringList);
    }

    public static StringList create(Collection<String> strings) {
        return new StringListImpl(Lists.newArrayList(strings));
    }

    public static StringList create(String[] stringArray) {
        List<String> strings = Lists.newArrayList(Arrays.asList(stringArray));

        return new StringListImpl(strings);
    }

    public static StringList create() {
        return new StringListImpl(Lists.newArrayList());
    }

    public static StringList charsToList(String string) {
        List<String> charsAsString = Lists.newArrayList();
        for (Character character : string.toCharArray()) {
            charsAsString.add(character.toString());
        }

        return create(charsAsString);
    }

    public Stream<String> stream() {
        return values.stream();
    }

    public static List<String> getAllValues(StringList... stringLists) {
        List<String> values = Lists.newArrayList();
        for (StringList stringList : stringLists) {
            values.addAll(stringList.getValues());
        }
        return values;
    }

    public static StringList join(StringList... stringLists) {
        List<String> values = getAllValues(stringLists);
        return StringListImpl.create(values);
    }

    @SafeVarargs
    public static StringList join(List<String>... lists) {
        StringList stringList = StringListImpl.create();

        for (List<String> list : lists) {
            stringList.addAll(list);
        }

        return stringList;
    }

    private Character[] stringToCharacterArray(String string) {
        return string.chars().mapToObj(c -> (char) c).toArray(Character[]::new);
    }
}
