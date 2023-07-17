package com.sequenceiq.cloudbreak.rotation.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import java.util.List;

import javax.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SecretTypeValidatorTest {

    @Captor
    private ArgumentCaptor<String> errorMessageCaptor;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setup() {
        setupContext();
    }

    @Test
    void testValidateIfDuplication() {
        assertFalse(new SecretTypesValidator().isValid(List.of("TEST", "TEST"), context));
        assertEquals("There is at least one duplication in the request!", errorMessageCaptor.getValue());
    }

    @Test
    void testValidateIfInvalidType() {
        assertFalse(new SecretTypesValidator().isValid(List.of("TEST", "INVALID"), context));
        assertEquals("Invalid secret type, cannot map secrets [TEST, INVALID].", errorMessageCaptor.getValue());
    }

    @Test
    void testValidate() {
        assertTrue(new SecretTypesValidator().isValid(List.of("TEST", "TEST_2"), context));
    }

    private void setupContext() {
        lenient().doReturn(builder).when(context).buildConstraintViolationWithTemplate(errorMessageCaptor.capture());
        lenient().doReturn(context).when(builder).addConstraintViolation();
    }
}
