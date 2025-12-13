package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeprecatedValidatorTest {

    private DeprecatedValidator underTest;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @BeforeEach
    public void setUp() {
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new DeprecatedValidator();
    }

    @Test
    void shouldBeValidIfFieldIsEmpty() {
        assertTrue(underTest.isValid("", context));
        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void shouldBeValidIfFieldIsNull() {
        assertTrue(underTest.isValid(null, context));
        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void shouldBeInValidIfFieldHasWhiteSpace() {
        assertFalse(underTest.isValid(" ", context));
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void shouldValidationFailIfAnyValueIsSubmitted() {
        assertFalse(underTest.isValid("https://192.168.10.1/", context));
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void shouldValidationFailIfAnyStringIsSubmitted() {
        assertFalse(underTest.isValid("abcd", context));
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(any(String.class));
    }
}
