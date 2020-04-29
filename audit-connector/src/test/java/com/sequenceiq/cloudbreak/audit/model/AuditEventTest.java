package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AuditEventTest {
    public static final ActorCrn ACTOR = ActorCrn.builder().withActorCrn("crn:cdp:iam:us-west-1:1234:user:1").build();

    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName              account      actor  eventData  eventName     eventSource   id                                      requestId   sourceIp   valid  expectedThrowable               errorMessage
            { "All null",            null,        null,  null,      null,         null,         null,                                   null,       null,      false, IllegalArgumentException.class, "Account ID name must be provided."                                           },
            { "Null account",        null,        ACTOR, null,      "EVENT_NAME", "iam",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Account ID name must be provided."                                           },
            { "Empty account",       "",          ACTOR, null,      "EVENT_NAME", "iam",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Account ID name must be provided."                                           },
            { "Null actor",          "accountId", null,  null,      "EVENT_NAME", "iam",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, NullPointerException.class,     null                                                                          },
            { "Null eventName",      "accountId", ACTOR, null,      null,         "iam",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Event name must be provided."                                                },
            { "Empty eventName",     "accountId", ACTOR, null,      "",           "iam",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Event name must be provided."                                                },
            { "Invalid ID",          "accountId", ACTOR, null,      "EVENT_NAME", "iam",        "x",                                    null,       null,      false, IllegalArgumentException.class, "ID must be a valid UUID."                                                    },
            { "Invalid eventSource", "accountId", ACTOR, null,      "EVENT_NAME", "AIM",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Event source must be a valid service name as represented in a CRN."          },
            { "Valid",               "accountId", ACTOR, null,      "EVENT_NAME", "iam",        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      true,  null,                           null                                                                          },
            { "Valid Null ID",       "accountId", ACTOR, null,      "EVENT_NAME", "iam",        null,                                   null,       null,      true,  null,                           null                                                                          },
            { "Valid Empty ID",      "accountId", ACTOR, null,      "EVENT_NAME", "iam",        "",                                     null,       null,      true,  null,                           null                                                                          },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String accountId, ActorBase actor, EventData eventData, String eventName, String eventSource, String id,
            String requestId, String sourceIp,
            boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                AuditEvent.builder()
                        .withAccountId(accountId)
                        .withActor(actor)
                        .withEventData(eventData)
                        .withEventName(eventName)
                        .withEventSource(eventSource)
                        .withId(id)
                        .withRequestId(requestId)
                        .withSourceIp(sourceIp)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> AuditEvent.builder().build();

        assertConstruction(constructor, false, IllegalArgumentException.class, "Account ID name must be provided.");
    }

}
