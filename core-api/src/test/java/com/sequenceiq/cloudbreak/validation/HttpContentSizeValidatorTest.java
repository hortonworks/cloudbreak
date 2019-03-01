package com.sequenceiq.cloudbreak.validation;

import static com.sequenceiq.cloudbreak.validation.HttpContentSizeValidator.MAX_IN_BYTES;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.Response.StatusType;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.helper.HttpHelper;

@RunWith(MockitoJUnitRunner.class)
public class HttpContentSizeValidatorTest {

    private static final String INVALID_MESSAGE = "The value should be a valid URL and start with 'http(s)'!";

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

    @Before
    public void setUp() {
        when(statusType.getFamily()).thenReturn(Family.SUCCESSFUL);
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_IN_BYTES));
        when(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).thenReturn(constraintViolationBuilder);
    }

    @Test
    public void testUrlWrongProtocol() {
        assertFalse(underTest.isValid("ftp://protocol.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(eq(INVALID_MESSAGE));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlWithoutProtocol() {
        assertFalse(underTest.isValid("without.protocol.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1)).buildConstraintViolationWithTemplate(eq(INVALID_MESSAGE));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlNotSuccessful() {
        when(statusType.getFamily()).thenReturn(Family.OTHER);
        when(statusType.getReasonPhrase()).thenReturn("not available");

        assertFalse(underTest.isValid("http://valid.url.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq("Failed to get response by the specified URL 'http://valid.url.com' due to: 'not available'!"));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlFailsWithException() {
        when(httpHelper.getContentLength(anyString())).thenThrow(RuntimeException.class);

        assertFalse(underTest.isValid("http://content.not.available.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(1))
                .buildConstraintViolationWithTemplate(eq("Failed to get response by the specified URL!"));
        verify(constraintViolationBuilder, times(1)).addConstraintViolation();
    }

    @Test
    public void testUrlFailsWithMinusOne() {
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, -1));

        assertFalse(underTest.isValid("http://unknown.error.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testUrlFailsWithZero() {
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, 0));

        assertFalse(underTest.isValid("http://empty.content.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testUrlFailsWithMoreThanMax() {
        when(httpHelper.getContentLength(anyString())).thenReturn(new ImmutablePair<>(statusType, MAX_IN_BYTES + 1));

        assertFalse(underTest.isValid("http://big.content.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testUrlNull() {
        assertTrue(underTest.isValid(null, constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    public void testUrlOk() {
        assertTrue(underTest.isValid("http://good.content.com", constraintValidatorContext));

        verify(constraintValidatorContext, times(0)).buildConstraintViolationWithTemplate(anyString());
    }
}
