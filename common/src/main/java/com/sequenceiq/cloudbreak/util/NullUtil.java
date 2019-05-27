package com.sequenceiq.cloudbreak.util;

import java.util.function.Consumer;

public class NullUtil {

    private NullUtil() {
    }

    public static <T> void ifNotNull(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
