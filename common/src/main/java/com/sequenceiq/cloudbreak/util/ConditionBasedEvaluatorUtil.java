package com.sequenceiq.cloudbreak.util;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ConditionBasedEvaluatorUtil {

    private ConditionBasedEvaluatorUtil() {
    }

    public static <T> void evaluateIfTrueDoOtherwise(T value, Function<T, ? extends Boolean> condition, Consumer<T> doIfTrue, Consumer<T> doOtherwise) {
        if (condition.apply(value)) {
            doIfTrue.accept(value);
        } else {
            doOtherwise.accept(value);
        }
    }

    public static <T> void doIfTrue(T subject, Predicate<T> condition, Consumer<T> doIfTrue) {
        if (condition.test(subject)) {
            doIfTrue.accept(subject);
        }
    }

    public static <T> void doIfTrue(boolean condition, T subject,  Consumer<T> doIfTrue) {
        if (condition) {
            doIfTrue.accept(subject);
        }
    }

    public static <T> void doIfFalse(T subject, Predicate<T> condition, Consumer<T> doIfFalse) {
        if (!condition.test(subject)) {
            doIfFalse.accept(subject);
        }
    }

    public static <T> void doIfFalse(boolean condition, T subject,  Consumer<T> doIfFalse) {
        if (!condition)  {
            doIfFalse.accept(subject);
        }
    }

    public static <T extends Throwable> void throwIfTrue(boolean condition, Supplier<? extends T> exception) throws T {
        if (condition) {
            throw exception.get();
        }
    }

    public static <T extends Throwable> void throwIfFalse(boolean condition, Supplier<? extends T> exeption) throws T {
        if (!condition) {
            throw exeption.get();
        }
    }

}
