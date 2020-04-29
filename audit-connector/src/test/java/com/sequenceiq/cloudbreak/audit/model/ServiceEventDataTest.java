package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ServiceEventDataTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName           eventDetails  version  valid  expectedThrowable               errorMessage
            { "All null",         null,         null,    true,  null,                           null },
            { "Empty details",    "",           null,    true,  null,                           null },
            { "Details not JSON", "x",          null,    false, IllegalArgumentException.class, "Service Event Details must be a valid JSON." },
            { "Details JSON",     "{}",         null,    true,  null,                           null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String eventDetails, String version, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ServiceEventData.builder()
                        .withEventDetails(eventDetails)
                        .withVersion(version)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ServiceEventData.builder().build();

        assertConstruction(constructor, true, null, null);
    }
}
