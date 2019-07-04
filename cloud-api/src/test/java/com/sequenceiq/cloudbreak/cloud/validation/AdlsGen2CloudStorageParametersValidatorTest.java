package com.sequenceiq.cloudbreak.cloud.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.storage.AdlsGen2CloudStorageParameters;

public class AdlsGen2CloudStorageParametersValidatorTest {

    private final ConstraintValidatorContext context = createContextMock();

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

    private static ConstraintValidatorContext createContextMock() {
        ConstraintValidatorContext contextMock = mock(ConstraintValidatorContext.class);
        ConstraintValidatorContext.ConstraintViolationBuilder constraintViolationBuilderMock = mock(ConstraintValidatorContext.ConstraintViolationBuilder.class);
        ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilderContextMock
                = mock(ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext.class);

        when(contextMock.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(constraintViolationBuilderMock);
        when(constraintViolationBuilderMock.addPropertyNode(anyString()))
                .thenReturn(nodeBuilderContextMock);
        when(nodeBuilderContextMock.addConstraintViolation()).thenReturn(contextMock);
        return contextMock;
    }

}
