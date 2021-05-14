package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import javax.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class DeprecatedValidatorTest {

    private DeprecatedValidator underTest;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Before
    public void setUp() {
        openMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new DeprecatedValidator();
    }

    @Test
    public void shouldBeValidIfFieldIsEmpty() {
        assertTrue(underTest.isValid("", context));
        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void shouldBeValidIfFieldIsNull() {
        assertTrue(underTest.isValid(null, context));
        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void shouldBeInValidIfFieldHasWhiteSpace() {
        assertFalse(underTest.isValid(" ", context));
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void shouldValidationFailIfAnyValueIsSubmitted() {
        assertFalse(underTest.isValid("https://192.168.10.1/", context));
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void shouldValidationFailIfAnyStringIsSubmitted() {
        assertFalse(underTest.isValid("abcd", context));
        verify(context, atLeastOnce()).buildConstraintViolationWithTemplate(any(String.class));
    }
}