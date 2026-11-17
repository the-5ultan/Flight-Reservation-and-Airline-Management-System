package com.aeroreserve.generic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A custom, serializable, generic collection used throughout the domain model.
 * It demonstrates encapsulation, generics and iterator-based traversal while
 * remaining interchangeable with the Java Collections Framework.
 *
 * @param <T> element type
 */
public class CustomList<T> implements Iterable<T>, Serializable {

    private static final long serialVersionUID = 1L;

    private Object[] elements;
    private int size;

    public CustomList() {
        this.elements = new Object[8];
        this.size = 0;
    }

    public CustomList(java.util.Collection<? extends T> source) {
        this();
        if (source != null) {
            for (T item : source) {
                add(item);
            }
        }
    }

    public void add(T element) {
        ensureCapacity(size + 1);
        elements[size++] = element;
    }

    public void addAll(Iterable<? extends T> source) {
        if (source == null) {
            return;
        }
        for (T element : source) {
            add(element);
        }
    }

    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkIndex(index);
        return (T) elements[index];
    }

    public void set(int index, T element) {
        checkIndex(index);
        elements[index] = element;
    }

    public T remove(int index) {
        checkIndex(index);
        @SuppressWarnings("unchecked") T removed = (T) elements[index];
        int moved = size - index - 1;
        if (moved > 0) {
            System.arraycopy(elements, index + 1, elements, index, moved);
        }
        elements[--size] = null;
        return removed;
    }

    public boolean remove(T element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(elements[i], element)) {
                remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(T element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(elements[i], element)) {
                return true;
            }
        }
        return false;
    }

    public int indexOf(T element) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(elements[i], element)) {
                return i;
            }
        }
        return -1;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            elements[i] = null;
        }
        size = 0;
    }

    /** Traversal with a consumer, an alternative to the iterator. */
    public void forEachItem(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked") T element = (T) elements[i];
            action.accept(element);
        }
    }

    public CustomList<T> filter(Predicate<? super T> predicate) {
        Objects.requireNonNull(predicate);
        CustomList<T> result = new CustomList<>();
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked") T element = (T) elements[i];
            if (predicate.test(element)) {
                result.add(element);
            }
        }
        return result;
    }

    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked") T element = (T) elements[i];
            result.add(element);
        }
        return result;
    }

    public Object[] toArray() {
        Object[] copy = new Object[size];
        System.arraycopy(elements, 0, copy, 0, size);
        return copy;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                return cursor < size;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                @SuppressWarnings("unchecked") T element = (T) elements[cursor];
                cursor++;
                return element;
            }
        };
    }

    private void ensureCapacity(int required) {
        if (required <= elements.length) {
            return;
        }
        int newCapacity = Math.max(required, elements.length * 2);
        Object[] grown = new Object[newCapacity];
        System.arraycopy(elements, 0, grown, 0, size);
        elements = grown;
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index " + index + " outside size " + size);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CustomList[");
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(elements[i]);
        }
        return sb.append(']').toString();
    }
}
