package com.sequenceiq.cloudbreak.util;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class NullUtil {

    private NullUtil() {
    }

    public static <T extends Throwable> void throwIfNull(Object o, Supplier<? extends T> exception) throws T {
        if (o == null) {
            throw exception.get();
        }
    }

    public static <T> void doIfNotNull(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public static <T> void doIfNotNullOtherwise(T value, Consumer<T> consumer, Runnable callableUponNull) {
        if (value != null) {
            consumer.accept(value);
        } else {
            try {
                callableUponNull.run();
            } catch (Exception e) {
                throw new IllegalStateException("Unable to execute secondary action due to: ", e);
            }
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

    public static <R> R getIfNotNullOtherwise(R value, R toReturnIfValueIsNull) {
        if (value != null) {
            return value;
        }
        return toReturnIfValueIsNull;
    }

    public static <T, U, R> R getIfNotNull(T firstArgument, U secondArgument, BiFunction<T, U, R> consumer) {
        if (firstArgument != null && secondArgument != null) {
            return consumer.apply(firstArgument, secondArgument);
        }
        return null;
    }

    public static <K, V> void putIfPresent(Map<K, V> map, K key, V value) {
        if (map != null && key != null && value != null) {
            map.put(key, value);
        }
    }

    public static boolean allNull(Object... values) {
        if (values != null) {
            for (Object v : values) {
                if (v != null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean allNotNull(Object... values) {
        for (Object v : values) {
            if (v == null) {
                return false;
            }
        }
        return true;
    }

}
