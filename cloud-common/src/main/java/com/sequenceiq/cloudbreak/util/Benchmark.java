package com.sequenceiq.cloudbreak.util;

import org.slf4j.Logger;

import java.util.function.Supplier;
import java.util.stream.Stream;

public class Benchmark {
    private Benchmark() {
    }

    public static void measure(Runnable callback, Logger logger, String message, Object... params) {
        long start = System.currentTimeMillis();
        callback.run();
        logDuration(logger, message, start, params);
    }

    public static <T> T measure(Supplier<T> callback, Logger logger, String message, Object... params) {
        long start = System.currentTimeMillis();
        T resp = callback.get();
        logDuration(logger, message, start, params);
        return resp;
    }

    public static <T, E extends Exception> T checkedMeasure(SingleCheckedSupplier<T, E> callback, Logger logger, String message, Object... params) throws E {
        long start = System.currentTimeMillis();
        T resp = callback.get();
        logDuration(logger, message, start, params);
        return resp;
    }

    public static <T, E extends Exception, X extends Exception> T checkedMeasure(MultiCheckedSupplier<T, E, X> callback, Logger logger, String message,
            Object... params) throws E, X {
        long start = System.currentTimeMillis();
        T resp = callback.get();
        logDuration(logger, message, start, params);
        return resp;
    }

    private static void logDuration(Logger logger, String message, long start, Object[] params) {
        Object[] extParams = Stream.concat(Stream.of(System.currentTimeMillis() - start), Stream.of(params)).toArray();
        logger.debug(message, extParams);
    }

    @FunctionalInterface
    public interface SingleCheckedSupplier<T, E extends Exception> {
        T get() throws E;
    }

    @FunctionalInterface
    public interface MultiCheckedSupplier<T, E extends Exception, X extends Exception> {
        T get() throws E, X;
    }
}
