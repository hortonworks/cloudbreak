package com.sequenceiq.cloudbreak.util;

import java.util.function.Consumer;
import java.util.function.Function;

public class ConditionBasedEvaulatorUtil {

    private ConditionBasedEvaulatorUtil() {
    }

    public static <T> void evaluateIfTrueDoOtherwise(T value, Function<T, ? extends Boolean> condition, Consumer<T> doIfTrue, Consumer<T> doOtherwise) {
        if (condition.apply(value)) {
            doIfTrue.accept(value);
        } else {
            doOtherwise.accept(value);
        }
    }

}
