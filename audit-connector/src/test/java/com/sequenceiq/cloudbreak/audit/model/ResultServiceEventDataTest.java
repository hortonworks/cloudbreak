package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import java.util.List;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ResultServiceEventDataTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName              resourceCrns                                                                       resultDetails  valid  expectedThrowable             errorMessage
            { "All null",            null,                                                                              null,          false, NullPointerException.class,     null },
            { "CRN null",            null,                                                                              "",            false, NullPointerException.class,     null },
            { "One CRN is invalid",  List.of("crn:cdp:iam:us-west-1:1234:user:1", "2"),                                 null,          false, IllegalArgumentException.class, "All CRNs must be valid." },
            { "CRNs are valid",      List.of("crn:cdp:iam:us-west-1:1234:user:1", "crn:cdp:iam:us-west-1:1234:user:2"), null,          true,  null,                           null },
            { "Details is not JSON", List.of("crn:cdp:iam:us-west-1:1234:user:1"),                                      "notJson",     false, IllegalArgumentException.class, "Result Details must be a valid JSON." },
            { "Details is JSON",     List.of("crn:cdp:iam:us-west-1:1234:user:1"),                                      "{}",          true,  null,                           null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, List<String> resourceCrns, String resultDetails, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ResultServiceEventData.builder()
                        .withResourceCrns(resourceCrns)
                        .withResultDetails(resultDetails)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ResultServiceEventData.builder().build();

        assertConstruction(constructor, false, NullPointerException.class, null);
    }
}
