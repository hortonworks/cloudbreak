package com.sequenceiq.cloudbreak.rotation.validator;

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
public class SingleSecretTypeValidatorTest {

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
    void testDuplication() {
        assertFalse(new SingleSecretTypesValidator().isValid(List.of("TEST", "TEST"), context));
        assertEquals("There is at least one duplication in the request!", errorMessageCaptor.getValue());
    }

    @Test
    void testMultiSecretValidation() {
        assertFalse(new SingleSecretTypesValidator().isValid(List.of("TEST_2", "TEST"), context));
        assertEquals("Only single secret types allowed!", errorMessageCaptor.getValue());
    }

    @Test
    void testValidation() {
        assertTrue(new SingleSecretTypesValidator().isValid(List.of("TEST"), context));
    }

    private void setupContext() {
        lenient().doReturn(builder).when(context).buildConstraintViolationWithTemplate(errorMessageCaptor.capture());
        lenient().doReturn(context).when(builder).addConstraintViolation();
    }
}
