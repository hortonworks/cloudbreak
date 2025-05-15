package com.sequenceiq.environment.environment.validation.validators;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.sequenceiq.cloudbreak.validation.ValidationResult;

class ManagedIdentityRoleValidatorTest {

    private static final String DOC_LINK = " Refer to Cloudera documentation at https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/" +
            "mc-az-minimal-setup-for-cloud-storage.html#mc-az-minimal-setup-for-cloud-storage for the required setup.";

    private ManagedIdentityRoleValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new ManagedIdentityRoleValidator();
    }

    @Test
    void validateEncryptionRoleTestWhenNull() {
        ValidationResult validationResult = underTest.validateEncryptionRole(null);

        assertThat(validationResult.hasError()).isFalse();
    }

    @ParameterizedTest(name = "encryptionRole=\"{0}\"")
    @ValueSource(strings = {"", " "})
    void validateEncryptionRoleTestWhenBlank(String encryptionRole) {
        ValidationResult validationResult = underTest.validateEncryptionRole(encryptionRole);

        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getFormattedErrors()).isEqualTo(
                "If specified, the managed identity resource ID may not be empty or whitespace only." + DOC_LINK);
    }

    @Test
    void validateEncryptionRoleTestWhenMalformed() {
        ValidationResult validationResult = underTest.validateEncryptionRole("foo");

        assertThat(validationResult.hasError()).isTrue();
        assertThat(validationResult.getFormattedErrors()).isEqualTo(
                "Must be a full valid managed identity resource ID in the format of /subscriptions/[your-subscription-id]/resourceGroups/" +
                        "[your-resource-group]/providers/Microsoft.ManagedIdentity/userAssignedIdentities/[name-of-your-identity]." + DOC_LINK);
    }

}