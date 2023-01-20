package com.sequenceiq.cloudbreak.cloud.azure.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ReactiveUtils {

    private ReactiveUtils() {
    }

    public static String getErrorMessageOrConcatSuppressedMessages(Exception e) {
        if (e.getClass().getSimpleName().contains("CompositeException")) {
            return Arrays.stream(e.getSuppressed())
                    .filter(cause -> !e.getMessage().contains("#block terminated with an error"))
                    .map(Throwable::getMessage)
                    .collect(Collectors.joining(" "));
        }
        return e.getMessage();
    }

    public static <T> void waitAll(List<Mono<T>> actions) {
        Flux.mergeDelayError(1, actions.toArray(Mono[]::new)).blockLast();
    }
}
