package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.sequenceiq.cloudbreak.auth.crn.Crn;

class AuditEventTest {
    public static final ActorCrn ACTOR = ActorCrn.builder().withActorCrn("crn:cdp:iam:us-west-1:1234:user:1").build();

    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName              account      actor  eventData  eventName     eventSource   id                                      requestId   sourceIp   valid  expectedThrowable               errorMessage
            { "All null",            null,        null,  null,      null,         null,         null,                                   null,       null,      false, IllegalArgumentException.class, "Account ID name must be provided."                                           },
            { "Null account",        null,        ACTOR, null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Account ID name must be provided."                                           },
            { "Empty account",       "",          ACTOR, null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Account ID name must be provided."                                           },
            { "Null actor",          "accountId", null,  null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, NullPointerException.class,     null                                                                          },
            { "Null eventName",      "accountId", ACTOR, null,      null,         Crn.Service.IAM,        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      false, IllegalArgumentException.class, "Event name must be provided."                                                },
            { "Invalid ID",          "accountId", ACTOR, null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        "x",                                    null,       null,      false, IllegalArgumentException.class, "ID must be a valid UUID."                                                    },
            { "Valid",               "accountId", ACTOR, null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        "F94E78AE-EF50-4DCE-A871-3F9A3CCB7E14", null,       null,      true,  null,                           null                                                                          },
            { "Valid Null ID",       "accountId", ACTOR, null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        null,                                   null,       null,      true,  null,                           null                                                                          },
            { "Valid Empty ID",      "accountId", ACTOR, null,      AuditEventName.CREATE_DATAHUB_CLUSTER, Crn.Service.IAM,        "",                                     null,       null,      true,  null,                           null                                                                          },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String accountId, ActorBase actor, EventData eventData, AuditEventName eventName, Crn.Service eventSource, String id,
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
