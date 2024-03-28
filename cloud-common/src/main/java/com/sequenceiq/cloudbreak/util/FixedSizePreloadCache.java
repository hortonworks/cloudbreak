package com.sequenceiq.cloudbreak.util;

import java.util.Stack;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class FixedSizePreloadCache<E> {

    private final int maxSize;

    private final Stack<E> stack;

    private final Supplier<E> generator;

    private final Lock lock;

    public FixedSizePreloadCache(int maxSize, Supplier<E> generator) {
        stack = new Stack<>();
        this.maxSize = maxSize;
        this.generator = generator;
        lock = new ReentrantLock();
        IntStream.range(0, maxSize).parallel().forEach(i -> push(generator.get()));
    }

    public E pop() {
        E element;
        try {
            lock.lock();
            element = stack.isEmpty() ? null : stack.pop();
        } finally {
            lock.unlock();
        }
        if (element == null) {
            return generator.get();
        }
        ForkJoinPool.commonPool().submit(() -> push(generator.get()));
        return element;
    }

    public int size() {
        try {
            lock.lock();
            return stack.size();
        } finally {
            lock.unlock();
        }
    }

    private void push(E item) {
        try {
            lock.lock();
            if (stack.size() < maxSize) {
                stack.push(item);
            }
        } finally {
            lock.unlock();
        }
    }
}