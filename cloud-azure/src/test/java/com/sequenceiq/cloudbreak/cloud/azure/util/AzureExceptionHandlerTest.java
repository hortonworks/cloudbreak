package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementException;
import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;

@ExtendWith(MockitoExtension.class)
public class AzureExceptionHandlerTest {

    private static final AzureExceptionHandlerParameters HANDLE_ALL_EXCEPTIONS =
            AzureExceptionHandlerParameters.builder().withHandleAllExceptions(true).build();

    private static final AzureExceptionHandlerParameters HANDLE_NOT_FOUND_EXCEPTIONS =
            AzureExceptionHandlerParameters.builder().withHandleNotFound(true).build();

    AzureExceptionHandler underTest = new AzureExceptionHandler();

    @Test
    void handleException() {
        Supplier<String> stringSupplier = () -> "This works";

        String result = underTest.handleException(stringSupplier);

        assertEquals("This works", result);
    }

    @Test
    void handleExceptionWhenNotFoundThenReturnsNull() {
        Supplier<String> stringSupplier = () -> {
            throw getManagementException(HttpStatus.NOT_FOUND);
        };

        String result = underTest.handleException(stringSupplier);

        assertNull(result);
    }

    @Test
    void handleExceptionWhenManagementExceptionNotHandledThenThrowsOriginalException() {
        Supplier<String> stringSupplier = () -> {
            throw getManagementException(HttpStatus.FORBIDDEN);
        };

        Assertions.assertThatThrownBy(() -> underTest.handleException(stringSupplier))
                .isExactlyInstanceOf(ManagementException.class);
    }

    @Test
    void handleExceptionWhenMsalUnauthorizedThenThrowsProviderAuthenticationFailedException() {
        Supplier<String> stringSupplier = () -> {
            throw getMsalServiceException(HttpStatus.UNAUTHORIZED);
        };

        Assertions.assertThatThrownBy(() -> underTest.handleException(stringSupplier))
                .isExactlyInstanceOf(ProviderAuthenticationFailedException.class);

    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"UNAUTHORIZED"}, mode = EnumSource.Mode.EXCLUDE)
    void handleExceptionWhenOtherThanMsalExceptionUnauthorizedThenThrowsOriginalException(HttpStatus httpStatus) {
        Supplier<String> stringSupplier = () -> {
            throw getMsalServiceException(httpStatus);
        };

        Assertions.assertThatThrownBy(() -> underTest.handleException(stringSupplier))
                .isExactlyInstanceOf(MsalServiceException.class);
    }

    @Test
    void handleExceptionWithDefaultWhenNotFoundThenReturnsDefault() {
        Supplier<String> stringSupplier = () -> {
            throw getManagementException(HttpStatus.NOT_FOUND);
        };

        String result = underTest.handleException(stringSupplier, "default");

        assertEquals("default", result);
    }

    @Test
    void handleExceptionWithDefaultWhenManagementExceptionNotHandledThenThrowsOriginalException() {
        Supplier<String> stringSupplier = () -> {
            throw getManagementException(HttpStatus.FORBIDDEN);
        };

        Assertions.assertThatThrownBy(() -> underTest.handleException(stringSupplier, "default"))
                .isExactlyInstanceOf(ManagementException.class);
    }

    @Test
    void handleExceptionWithDefaultAndHandleAllExceptionsWhenManagementExceptionThenReturnsDefault() {
        Supplier<String> stringSupplier = () -> {
            throw new ManagementException("", null);
        };

        String result = underTest.handleException(stringSupplier, "default", HANDLE_ALL_EXCEPTIONS);

        assertEquals("default", result);
    }

    @Test
    void handleExceptionWithDefaultAndHandleAllExceptionsWhenNotManagementExceptionThenThrowsOriginalException() {
        Supplier<String> stringSupplier = () -> {
            throw new RuntimeException("my Exception");
        };

        Assertions.assertThatThrownBy(() -> underTest.handleException(stringSupplier, "default", HANDLE_ALL_EXCEPTIONS))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("my Exception");
    }

    @Test
    void handleExceptionWithDefaultAndHandleNotFoundExceptionsWhenNotFoundThenReturnsDefault() {
        Supplier<String> stringSupplier = () -> {
            throw getManagementException(HttpStatus.NOT_FOUND);
        };

        String result = underTest.handleException(stringSupplier, "default", HANDLE_NOT_FOUND_EXCEPTIONS);

        assertEquals("default", result);
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"NOT_FOUND"}, mode = EnumSource.Mode.EXCLUDE)
    void handleExceptionWithDefaultAndHandleNotFoundWhenOtherThanNotFoundThenThrowsOriginalException(HttpStatus httpStatus) {
        Supplier<String> stringSupplier = () -> {
            throw getManagementException(httpStatus);
        };

        Assertions.assertThatThrownBy(() -> underTest.handleException(stringSupplier, "default", HANDLE_NOT_FOUND_EXCEPTIONS))
                .isExactlyInstanceOf(ManagementException.class);
    }

    private ManagementException getManagementException(HttpStatus httpStatus) {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(httpStatus.value());
        return new ManagementException("", httpResponse);
    }

    private MsalServiceException getMsalServiceException(HttpStatus httpStatus) {
        MsalServiceException msalServiceException = mock(MsalServiceException.class);
        when(msalServiceException.statusCode()).thenReturn(httpStatus.value());
        when(msalServiceException.getSuppressed()).thenReturn(new Throwable[]{});
        return msalServiceException;
    }

}
