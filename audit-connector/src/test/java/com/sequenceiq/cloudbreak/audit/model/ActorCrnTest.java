package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ActorCrnTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName      crn                                     valid  expectedThrowable               errorMessage
            { "Null CRN",    null,                                   false, IllegalArgumentException.class, "Actor user must be a valid CRN" },
            { "Empty CRN",   "",                                     false, IllegalArgumentException.class, "Actor user must be a valid CRN" },
            { "Invalid CRN", "notcrn",                               false, IllegalArgumentException.class, "Actor user must be a valid CRN" },
            { "Valid CRN",   "crn:cdp:iam:us-west-1:1234:user:5678", true,  null,                           null                             },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String crn, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ActorCrn.builder()
                        .withActorCrn(crn)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ActorCrn.builder().build();

        assertConstruction(constructor, false, IllegalArgumentException.class, "Actor user must be a valid CRN");
    }
}
