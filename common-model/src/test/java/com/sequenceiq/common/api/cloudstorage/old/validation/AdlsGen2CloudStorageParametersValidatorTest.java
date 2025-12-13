package com.sequenceiq.common.api.cloudstorage.old.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.Test;

import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.util.ContextMockUtil;

public class AdlsGen2CloudStorageParametersValidatorTest {

    private static final String MANAGED_IDENTITY_ID =
            "/subscriptions/a9d4456e-349f-44f6-bc73-54a8d523e504/resourceGroups"
                    + "/testrg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/user2";

    private final ConstraintValidatorContext context = ContextMockUtil.createContextMock();

    private final AdlsGen2CloudStorageParametersValidator validator = new AdlsGen2CloudStorageParametersValidator();

    @Test
    public void testAdlsGen2ParametersValidationWithLongAccountName() {
        assertFalse(testValidator(create("ab", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithShortAccountName() {
        assertFalse(testValidator(create("abcdefghijklm123456789123456789", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithInvalidAccountName() {
        assertFalse(testValidator(create(":?<>;/", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithNullAccountKey() {
        assertFalse(testValidator(create("validaccountname", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithNullParams() {
        assertFalse(testValidator(create(null, null, null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithManagedIdentity() {
        assertTrue(testValidator(create(null, null, MANAGED_IDENTITY_ID)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithInvalidAccountKey() {
        assertFalse(testValidator(create("validaccountname", "';'';';;'][['][")));
    }

    @Test
    public void testAdlsGen2ParametersValidation() {
        assertTrue(testValidator(create("validaccountname",
                "utgWEh7k/rB7CAwSsTWY8tjskMJc5N4glKm+DYpRvdnQ0kOy5l04kvPeFmQMQjQhJvjCwVZtmPk/fvORZP3zwCR=")));
    }

    private boolean testValidator(AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters) {
        return validator.isValid(adlsGen2CloudStorageParameters, context);
    }

    private AdlsGen2CloudStorageV1Parameters create(String accountName, String accountKey) {
        return create(accountName, accountKey, null);
    }

    private AdlsGen2CloudStorageV1Parameters create(String accountName, String accountKey, String managedIdentity) {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageParameters.setAccountKey(accountKey);
        adlsGen2CloudStorageParameters.setAccountName(accountName);
        adlsGen2CloudStorageParameters.setManagedIdentity(managedIdentity);
        return adlsGen2CloudStorageParameters;
    }

}
