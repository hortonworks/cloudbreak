package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
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
class JdbcConnectionUrlValidatorTest {

    private JdbcConnectionUrlValidator underTest;

    @Mock
    private ValidJdbcConnectionUrl annotation;

    @Mock(answer = RETURNS_DEEP_STUBS)
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        when(context.buildConstraintViolationWithTemplate(any(String.class)).addConstraintViolation()).thenReturn(context);

        underTest = new JdbcConnectionUrlValidator();
    }

    @Test
    void testValidWithDatabase() {
        when(annotation.databaseExpected()).thenReturn(true);

        underTest.initialize(annotation);
        assertTrue(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306/hivedb", context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testValidWithoutDatabase() {
        when(annotation.databaseExpected()).thenReturn(false);

        underTest.initialize(annotation);
        assertTrue(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306", context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testInvalid() {
        when(annotation.databaseExpected()).thenReturn(true);

        underTest.initialize(annotation);
        assertFalse(underTest.isValid("foo", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testInvalidDatabaseExpected() {
        when(annotation.databaseExpected()).thenReturn(true);

        underTest.initialize(annotation);
        assertFalse(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testInvalidDatabaseNotExpected() {
        when(annotation.databaseExpected()).thenReturn(false);

        underTest.initialize(annotation);
        assertFalse(underTest.isValid("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:3306/hivedb", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testNullIsValid() {
        assertTrue(underTest.isValid(null, context));

        verify(context, never()).buildConstraintViolationWithTemplate(any(String.class));
    }

    @Test
    void testEmptyIsInvalid() {
        assertFalse(underTest.isValid("", context));

        verify(context).buildConstraintViolationWithTemplate(any(String.class));
    }
}
