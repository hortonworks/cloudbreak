package com.sequenceiq.cloudbreak.util;

import java.util.function.Consumer;
import java.util.function.Function;

public class NullUtil {

    private NullUtil() {
    }

    public static <T> void ifNotNull(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <T, R> R ifNotNullF(T value, Function<T, R> consumer) {
        if (value != null) {
            return consumer.apply(value);
        }
        return null;
    }
}
