package com.sequenceiq.common.api.cloudstorage.validation;

import javax.validation.ConstraintValidatorContext;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.common.api.cloudstorage.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.util.ContextMockUtil;

public class AdlsGen2CloudStorageParametersValidatorTest {

    private final ConstraintValidatorContext context = ContextMockUtil.createContextMock();

    private final AdlsGen2CloudStorageParametersValidator validator = new AdlsGen2CloudStorageParametersValidator();

    @Test
    public void testAdlsGen2ParametersValidationWithLongAccountName() {
        Assert.assertFalse(testValidator(create("ab", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithShortAccountName() {
        Assert.assertFalse(testValidator(create("abcdefghijklm123456789123456789", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithInvalidAccountName() {
        Assert.assertFalse(testValidator(create(":?<>;/", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithNullAccountKey() {
        Assert.assertFalse(testValidator(create("validaccountname", null)));
    }

    @Test
    public void testAdlsGen2ParametersValidationWithInvalidAccountKey() {
        Assert.assertFalse(testValidator(create("validaccountname", "';'';';;'][['][")));
    }

    @Test
    public void testAdlsGen2ParametersValidation() {
        Assert.assertTrue(testValidator(create("validaccountname",
                "utgWEh7k/rB7CAwSsTWY8tjskMJc5N4glKm+DYpRvdnQ0kOy5l04kvPeFmQMQjQhJvjCwVZtmPk/fvORZP3zwCR=")));
    }

    private boolean testValidator(AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters) {
        return validator.isValid(adlsGen2CloudStorageParameters, context);
    }

    private AdlsGen2CloudStorageV1Parameters create(String accountName, String accountKey) {
        AdlsGen2CloudStorageV1Parameters adlsGen2CloudStorageParameters = new AdlsGen2CloudStorageV1Parameters();
        adlsGen2CloudStorageParameters.setAccountKey(accountKey);
        adlsGen2CloudStorageParameters.setAccountName(accountName);
        return adlsGen2CloudStorageParameters;
    }

}
