package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ApiRequestDataTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName      apiVersion  mutating  requestParams  userAgent  valid  expectedThrowable  errorMessage
            { "All null",    null,       false,    null,          null,      true,  null,              null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String apiVersion, boolean mutating, String requestParameters, String userAgent,
            boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ApiRequestData.builder()
                        .withApiVersion(apiVersion)
                        .withMutating(mutating)
                        .withRequestParameters(requestParameters)
                        .withUserAgent(userAgent)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ApiRequestData.builder().build();

        assertConstruction(constructor, true, null, null);
    }
}
