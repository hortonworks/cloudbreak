package com.sequenceiq.cloudbreak.audit.model;

import static com.sequenceiq.cloudbreak.audit.util.TestUtil.assertConstruction;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigInfoTest {

    // @formatter:off
    // CHECKSTYLE:OFF
    static Object[][] scenarios() {
        return new Object[][] {
            // testName              actorCrn                                    credentialName  enabled  requestId  storageLocation  storageRegion   valid  expectedThrowable               errorMessage
            { "All null",            null,                                       null,           false,   null,      null,             null,          false, IllegalArgumentException.class, "Actor CRN must be valid." },
            { "Null CRN",            "x",                                       "credName",      false,   null,      "location",       null,          false, IllegalArgumentException.class, "Actor CRN must be valid." },
            { "Empty CRN",           "x",                                       "credName",      false,   null,      "location",       null,          false, IllegalArgumentException.class, "Actor CRN must be valid." },
            { "Invalid CRN",         "x",                                       "credName",      false,   null,      "location",       null,          false, IllegalArgumentException.class, "Actor CRN must be valid." },
            { "Null Credential",     "crn:cdp:iam:us-west-1:1234:user:5678",    null,            false,   null,      "location",       null,          false, IllegalArgumentException.class, "Credential name must be provided." },
            { "Empty Credential",    "crn:cdp:iam:us-west-1:1234:user:5678",     "",             false,   null,      "location",       null,          false, IllegalArgumentException.class, "Credential name must be provided." },
            { "Null Storage location", "crn:cdp:iam:us-west-1:1234:user:5678",  "credName",      false,   null,      null,             null,          false, IllegalArgumentException.class, "Storage location must be provided." },
            { "Empty Storage location", "crn:cdp:iam:us-west-1:1234:user:5678", "credName",      false,   null,      "",               null,          false, IllegalArgumentException.class, "Storage location must be provided." },
            { "All required fields", "crn:cdp:iam:us-west-1:1234:user:5678",    "credName",      false,   null,      "location",       null,          true,  null,                           null },
        };
    }
    // CHECKSTYLE:ON
    // @formatter:on

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void constructionTest(String testName, String actorCrn, String credentialName, boolean enabled, String requestId,
            String storageLocation, String storageRegion, boolean valid, Class<?> expectedThrowable, String errorMessage) {

        ThrowingCallable constructor = () ->
                ConfigInfo.builder()
                        .withActorCrn(actorCrn)
                        .withCredentialName(credentialName)
                        .withEnabled(enabled)
                        .withRequestId(requestId)
                        .withStorageLocation(storageLocation)
                        .withStorageRegion(storageRegion)
                        .build();

        assertConstruction(constructor, valid, expectedThrowable, errorMessage);
    }

    @Test
    void emptyBuilderConstructionTest() {

        ThrowingCallable constructor = () -> ConfigInfo.builder().build();

        assertConstruction(constructor, false, IllegalArgumentException.class, "Actor CRN must be valid.");
    }
}
