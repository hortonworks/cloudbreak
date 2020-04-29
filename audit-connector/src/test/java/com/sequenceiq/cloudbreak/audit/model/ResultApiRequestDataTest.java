package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResultApiRequestDataTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName      responseParameters  valid  expectedThrowable  errorMessage
            { "All null",    null,               true,  null,              null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String responseParameters, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ResultApiRequestData.builder()
                        .withResponseParameters(responseParameters)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ResultApiRequestData.builder().build();

        assertConstruction(constructor, true, null, null);
    }
}
