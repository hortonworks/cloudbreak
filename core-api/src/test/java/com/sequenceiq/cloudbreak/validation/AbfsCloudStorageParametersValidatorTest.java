package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.v2.filesystem.AbfsCloudStorageParameters;

public class AbfsCloudStorageParametersValidatorTest {

    private final ConstraintValidatorContext context = ContextMockUtil.createContextMock();

    private final AbfsCloudStorageParametersValidator validator = new AbfsCloudStorageParametersValidator();

    @Test
    public void testAbfsParametersValidationWithLongAccountName() {
        assertFalse(testValidator(create("ab", null)));
    }

    @Test
    public void testAbfsParametersValidationWithShortAccountName() {
        assertFalse(testValidator(create("abcdefghijklm123456789123456789", null)));
    }

    @Test
    public void testAbfsParametersValidationWithInvalidAccountName() {
        assertFalse(testValidator(create(":?<>;/", null)));
    }

    @Test
    public void testAbfsParametersValidationWithNullAccountKey() {
        assertFalse(testValidator(create("validaccountname", null)));
    }

    @Test
    public void testAbfsParametersValidationWithInvalidAccountKey() {
        assertFalse(testValidator(create("validaccountname", "';'';';;'][['][")));
    }

    @Test
    public void testAbfsParametersValidation() {
        assertTrue(testValidator(create("validaccountname",
                "utgWEh7k/rB7CAwSsTWY8tjskMJc5N4glKm+DYpRvdnQ0kOy5l04kvPeFmQMQjQhJvjCwVZtmPk/fvORZP3zwCR=")));
    }

    private boolean testValidator(AbfsCloudStorageParameters abfsCloudStorageParameters) {
        return validator.isValid(abfsCloudStorageParameters, context);
    }

    private AbfsCloudStorageParameters create(String accountName, String accountKey) {
        AbfsCloudStorageParameters abfsCloudStorageParameters = new AbfsCloudStorageParameters();
        abfsCloudStorageParameters.setAccountKey(accountKey);
        abfsCloudStorageParameters.setAccountName(accountName);
        return abfsCloudStorageParameters;
    }

}
