package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveUtils {

    private static final String BLOCK_TERMINATED_WITH_ERROR = "#block terminated with an error";

    private ReactiveUtils() {
    }

    public static String getErrorMessageOrConcatSuppressedMessages(Exception e) {
        if (Exceptions.isMultiple(e)) {
            return String.join(" ", getErrorMessages(e));
        } else {
            return Exceptions.unwrap(e).getMessage();
        }
    }

    public static List<String> getErrorMessages(Exception e) {
        if (Exceptions.isMultiple(e)) {
            return Arrays.stream(e.getSuppressed())
                    .map(Throwable::getMessage)
                    .filter(Objects::nonNull)
                    .filter(message -> !message.contains(BLOCK_TERMINATED_WITH_ERROR))
                    .toList();
        } else {
            return Stream.ofNullable(Exceptions.unwrap(e).getMessage()).toList();
        }
    }

    public static <T> void waitAll(List<Mono<T>> actions) {
        Flux.mergeDelayError(1, actions.toArray(Mono[]::new)).blockLast();
    }
}
