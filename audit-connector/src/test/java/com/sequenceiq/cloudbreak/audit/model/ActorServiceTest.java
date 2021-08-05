package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ActorServiceTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName        serviceName   valid  expectedThrowable,              errorMessage
            { "Null Service Name",    null,  false, NullPointerException.class,     "input should not be null."                                                                      },
            { "Empty Service Name",   "",    false, IllegalArgumentException.class, "Actor service name must be a valid service name as represented in a CRN" },
            { "Invalid Service Name", "aim", false, IllegalArgumentException.class, "Actor service name must be a valid service name as represented in a CRN" },
            { "Valid Service Name",   "iam", true,  null,                           null                                                                      },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String serviceName, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ActorService.builder()
                        .withActorServiceName(serviceName)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ActorService.builder().build();

        assertConstruction(constructor, false, NullPointerException.class, "input should not be null.");
    }
}
