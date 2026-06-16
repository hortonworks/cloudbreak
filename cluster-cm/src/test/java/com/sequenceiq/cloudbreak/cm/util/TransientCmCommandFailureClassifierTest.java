package com.sequenceiq.cloudbreak.cm.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TransientCmCommandFailureClassifierTest {

    private final TransientCmCommandFailureClassifier underTest = new TransientCmCommandFailureClassifier();

    @ParameterizedTest
    @ValueSource(strings = {
            "ipa: ERROR: Operations error: Error checking for attribute uniqueness.",
            "GenerateCredentials(id=42): HTTP/azure-test-80fe-compute7 service_add returned DatabaseError",
            "error checking for attribute uniqueness",
            "DATABASEERROR"
    })
    void isTransientCredentialGenerationFailureReturnsTrueForKnownFreeIpaErrors(String message) {
        assertTrue(underTest.isTransientCredentialGenerationFailure(message));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Role is missing Kerberos keytab.",
            "Failed to start service.",
            "Some unrelated terminal failure"
    })
    void isTransientCredentialGenerationFailureReturnsFalseForOtherErrors(String message) {
        assertFalse(underTest.isTransientCredentialGenerationFailure(message));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void isTransientCredentialGenerationFailureReturnsFalseForBlankMessage(String message) {
        assertFalse(underTest.isTransientCredentialGenerationFailure(message));
    }

    @Test
    void isTransientCredentialGenerationFailureReturnsFalseWhenFragmentAbsent() {
        assertFalse(underTest.isTransientCredentialGenerationFailure("Cloudera Manager command [Apply host template] failed"));
    }
}
