package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.filesystems.requests.adls0.AdlsGen2CloudStorageParameters;

public class AdlsGen2CloudStorageParametersValidatorTest {

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
    public void testAdlsGen2ParametersValidationWithInvalidAccountKey() {
        assertFalse(testValidator(create("validaccountname", "';'';';;'][['][")));
    }

    @Test
    public void testAdlsGen2ParametersValidation() {
        assertTrue(testValidator(create("validaccountname",
                "utgWEh7k/rB7CAwSsTWY8tjskMJc5N4glKm+DYpRvdnQ0kOy5l04kvPeFmQMQjQhJvjCwVZtmPk/fvORZP3zwCR=")));
    }

    private boolean testValidator(AdlsGen2CloudStorageParameters adlsGen2CloudStorageParameters) {
        return validator.isValid(adlsGen2CloudStorageParameters, context);
    }

    private AdlsGen2CloudStorageParameters create(String accountName, String accountKey) {
        AdlsGen2CloudStorageParameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageParameters();
        adlsGen2CloudStorageParameters.setAccountKey(accountKey);
        adlsGen2CloudStorageParameters.setAccountName(accountName);
        return adlsGen2CloudStorageParameters;
    }

}
