package api;

import com.google.common.collect.Lists;

import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Stream;

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
    public Iterator iterator() {
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

    public static StringList create(String string, String regex) {
        String[] stringList = string.split(regex);
        return create(stringList);
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
}
