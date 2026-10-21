package com.sequenceiq.cloudbreak.cloud.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class InsufficientCapacityExceptionTest {

    @Test
    void allInstanceTypesExhaustedBuildsMessageWithOperationInstanceTypesAndReason() {
        RuntimeException cause = new RuntimeException("cause");

        InsufficientCapacityException exception = InsufficientCapacityException.allInstanceTypesExhausted("Stack creation failed",
                List.of("type1", "type2"), "no capacity", cause);

        assertThat(exception)
                .hasMessage("Stack creation failed: all instance types [type1, type2] exhausted due to capacity shortage. Reason: no capacity")
                .hasCause(cause);
    }

    @Test
    void allInstanceTypesExhaustedAllowsMissingCause() {
        InsufficientCapacityException exception = InsufficientCapacityException.allInstanceTypesExhausted("Stack creation failed",
                List.of("type1"), "simulated", null);

        assertThat(exception).hasNoCause();
    }
}
