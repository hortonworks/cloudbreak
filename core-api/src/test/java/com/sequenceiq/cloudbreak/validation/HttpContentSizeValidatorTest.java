package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;

@ExtendWith(MockitoExtension.class)
public class HttpContentSizeValidatorTest {

    private static final String INVALID_MESSAGE = "The value should be a valid URL and start with 'http(s)'!";

    private static final int MAX_SIZE = 6 * 1024 * 1024;

    @InjectMocks
    private HttpContentSizeValidator underTest;

    @Mock
    private HttpHelper httpHelper;

    @Mock
    private StatusType statusType;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ConstraintViolationBuilder constraintViolationBuilder;

    @Mock
    private ContentSizeProvider contentSizeProvider;

    @Test
    public void testUrlWrongProtocol() {
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid("ftp://protocol.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(eq(INVALID_MESSAGE));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlWithoutProtocol() {
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        assertFalse(underTest.isValid("without.protocol.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(eq(INVALID_MESSAGE));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlNotSuccessful() {
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE));
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        when(statusType.getFamily()).thenReturn(Family.OTHER);
        when(statusType.getReasonPhrase()).thenReturn("not available");

        assertFalse(underTest.isValid("http://valid.url.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq("Failed to get response by the specified URL 'http://valid.url.com' due to: 'not available'!"));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlFailsWithException() {
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE));
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        when(httpHelper.getContentLength(anyString())).thenThrow(RuntimeException.class);

        assertFalse(underTest.isValid("http://content.not.available.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq("Failed to get response by the specified URL!"));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlFailsWithMinusOne() {
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE));
        when(contentSizeProvider.getMaxSizeInBytes()).thenReturn(MAX_SIZE);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, -1));

        assertFalse(underTest.isValid("http://unknown.error.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(anyString());
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlFailsWithZero() {
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE));
        when(contentSizeProvider.getMaxSizeInBytes()).thenReturn(MAX_SIZE);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, 0));

        assertFalse(underTest.isValid("http://empty.content.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(anyString());
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlFailsWithMoreThanMax() {
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE));
        when(contentSizeProvider.getMaxSizeInBytes()).thenReturn(MAX_SIZE);
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);

        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE + 1));

        assertFalse(underTest.isValid("http://big.content.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(anyString());
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlNull() {
        assertTrue(underTest.isValid(null, constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testUrlOk() {
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_SIZE));
        when(contentSizeProvider.getMaxSizeInBytes()).thenReturn(MAX_SIZE);

        assertTrue(underTest.isValid("http://good.content.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }
}
