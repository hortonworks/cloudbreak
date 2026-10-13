package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

import java.util.List;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Mono;

class ReactiveUtilsTest {

    @Test
    void testCompositeExceptionMessagesAreConcatenatedWithoutBlockMarkerAndNullMessages() {
        Exception exception = catchException(() -> ReactiveUtils.waitAll(List.of(
                Mono.empty(),
                Mono.error(new RuntimeException("first")),
                Mono.error(new RuntimeException()),
                Mono.error(new RuntimeException("second")))));

        assertThat(ReactiveUtils.getErrorMessageOrConcatSuppressedMessages(exception)).isEqualTo("first second");
    }

    @Test
    void testSingleFailureReturnsItsMessage() {
        Exception exception = catchException(() -> ReactiveUtils.waitAll(List.of(Mono.empty(), Mono.error(new RuntimeException("only")))));

        assertThat(ReactiveUtils.getErrorMessageOrConcatSuppressedMessages(exception)).isEqualTo("only");
    }

    @Test
    void testSingleCheckedFailureReturnsItsOwnMessage() {
        Exception exception = catchException(() -> ReactiveUtils.waitAll(List.of(Mono.empty(), Mono.error(new TimeoutException("timeout")))));

        assertThat(ReactiveUtils.getErrorMessageOrConcatSuppressedMessages(exception)).isEqualTo("timeout");
    }

    @Test
    void testNonCompositeExceptionWithNullMessage() {
        assertThat(ReactiveUtils.getErrorMessageOrConcatSuppressedMessages(new RuntimeException())).isNull();
    }
}
