package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import javax.validation.ConstraintValidatorContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class JdbcConnectionUrlValidatorTest {

    private JdbcConnectionUrlValidator underTest;

    @Mock
    private ValidJdbcConnectionUrl annotation;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @Before
    public void setUp() {
        initMocks(this);
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new JdbcConnectionUrlValidator();
    }

    @Test
    public void testValidWithDatabase() {
        when(annotation.databaseExpected()).thenReturn(true);

        underTest.initialize(annotation);
        assertTrue(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306/hivedb", context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testValidWithoutDatabase() {
        when(annotation.databaseExpected()).thenReturn(false);

        underTest.initialize(annotation);
        assertTrue(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306", context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testInvalid() {
        when(annotation.databaseExpected()).thenReturn(true);

        underTest.initialize(annotation);
        assertFalse(underTest.isValid("foo", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testInvalidDatabaseExpected() {
        when(annotation.databaseExpected()).thenReturn(true);

        underTest.initialize(annotation);
        assertFalse(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    public void testInvalidDatabaseNotExpected() {
        when(annotation.databaseExpected()).thenReturn(false);

        underTest.initialize(annotation);
        assertFalse(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306/hivedb", context));

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
