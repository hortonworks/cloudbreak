package com.sequenceiq.cloudbreak.util;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NullUtil {

    private NullUtil() {
    }

    public static <T extends Throwable> void throwIfNull(Object o, Supplier<? extends T> exeption) throws T {
        if (Objects.isNull(o)) {
            throw exeption.get();
        }
    }

    public static <T> void doIfNotNull(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <T, R> R getIfNotNull(T value, Function<T, R> consumer) {
        if (value != null) {
            return consumer.apply(value);
        }
        return null;
    }

    public static <T, R> R getIfNotNullOtherwise(T value, Function<T, R> consumer, R toReturnIfValueIsNull) {
        if (value != null) {
            return consumer.apply(value);
        }
        return toReturnIfValueIsNull;
    }

    public static <T, U, R> R getIfNotNull(T firstArgument, U secondArgument, BiFunction<T, U, R> consumer) {
        if (firstArgument != null && secondArgument != null) {
            return consumer.apply(firstArgument, secondArgument);
        }
        return null;
    }
}
