package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AttemptAuditEventResultTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName            actorCrn                             id                                      requestId   resultCode  resultEventData   resultMessage  valid  expectedThrowable               errorMessage
            { "All null",          null,                                null,                                   null,       null,       null,             null,          false, IllegalArgumentException.class, "ID must be a valid UUID." },
            { "Null ID",           "crn:cdp:iam:us-west-1:1234:user:1", null,                                   null,       "code",     null,             null,          false, IllegalArgumentException.class, "ID must be a valid UUID." },
            { "Empty ID",          "crn:cdp:iam:us-west-1:1234:user:1", "",                                     null,       "code",     null,             null,          false, IllegalArgumentException.class, "ID must be a valid UUID." },
            { "Invalid ID",        "crn:cdp:iam:us-west-1:1234:user:1", "x",                                    null,       "code",     null,             null,          false, IllegalArgumentException.class, "ID must be a valid UUID." },
            { "Null ActorCRN",     null,                                "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       "code",     null,             null,          false, IllegalArgumentException.class, "Actor user must be a valid CRN." },
            { "Empty ActorCRN",    "",                                  "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       "code",     null,             null,          false, IllegalArgumentException.class, "Actor user must be a valid CRN." },
            { "Invalid ActorCRN",  "x",                                 "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       "code",     null,             null,          false, IllegalArgumentException.class, "Actor user must be a valid CRN." },
            { "Null result code",  "crn:cdp:iam:us-west-1:1234:user:1", "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,       null,             null,          false, IllegalArgumentException.class, "Result code must be provided." },
            { "Empty result code", "crn:cdp:iam:us-west-1:1234:user:1", "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       "",         null,             null,          false, IllegalArgumentException.class, "Result code must be provided." },
            { "Valid",             "crn:cdp:iam:us-west-1:1234:user:1", "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       "code",     null,             null,          true,  null,                           null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String actorCrn, String id, String requestId, String resultCode, ResultEventData resultEventData,
            String resultMessage, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                AttemptAuditEventResult.builder()
                        .withActorCrn(actorCrn)
                        .withId(id)
                        .withRequestId(requestId)
                        .withResultCode(resultCode)
                        .withResultEventData(resultEventData)
                        .withResultMessage(resultMessage)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> AttemptAuditEventResult.builder().build();

        assertConstruction(constructor, false, IllegalArgumentException.class, "ID must be a valid UUID.");
    }
}
