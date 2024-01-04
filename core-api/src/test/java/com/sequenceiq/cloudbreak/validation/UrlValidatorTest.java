package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import jakarta.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class UrlValidatorTest {

    private UrlValidator underTest;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Before
    public void setUp() {
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new UrlValidator();
    }

    @Test
    public void testValid() {
        assertTrue(underTest.isValid("https://www.cloudera.com/", context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testLocalUrlIsValid() {
        assertTrue(underTest.isValid("http://localhost:7189/", context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testInvalid() {
        assertFalse(underTest.isValid("foo", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testNullIsValid() {
        assertTrue(underTest.isValid(null, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testEmptyIsInvalid() {
        assertFalse(underTest.isValid("", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }
}
