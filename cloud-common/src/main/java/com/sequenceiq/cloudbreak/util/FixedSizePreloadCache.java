package com.sequenceiq.cloudbreak.util;

import java.util.Stack;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class FixedSizePreloadCache<E> {

    private final int maxSize;

    private final Stack<E> stack;

    private final Supplier<E> generator;

    public FixedSizePreloadCache(int maxSize, Supplier<E> generator) {
        stack = new Stack<>();
        this.maxSize = maxSize;
        this.generator = generator;

        IntStream.range(0, maxSize).parallel().forEach(i -> push(generator.get()));
    }

    public E pop() {
        E element;
        synchronized (stack) {
            element = stack.isEmpty() ? null : stack.pop();
        }
        if (element == null) {
            return generator.get();
        }
        new Thread(() -> push(generator.get())).start();
        return element;
    }

    public int size() {
        synchronized (stack) {
            return stack.size();
        }
    }

    private void push(E item) {
        synchronized (stack) {
            if (stack.size() < maxSize) {
                stack.push(item);
            }
        }
    }
}