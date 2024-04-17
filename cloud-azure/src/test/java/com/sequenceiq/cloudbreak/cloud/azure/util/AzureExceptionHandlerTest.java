package com.sequenceiq.cloudbreak.cloud.azure.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementError;
import com.azure.core.management.exception.ManagementException;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.microsoft.aad.msal4j.MsalServiceException;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;

@ExtendWith(MockitoExtension.class)
public class AzureExceptionHandlerTest {

    private static final AzureExceptionHandlerParameters HANDLE_ALL_EXCEPTIONS =
            AzureExceptionHandlerParameters.builder().withHandleAllExceptions(true).build();

    private static final AzureExceptionHandlerParameters HANDLE_NOT_FOUND_EXCEPTIONS =
            AzureExceptionHandlerParameters.builder().withHandleNotFound(true).build();

    AzureExceptionHandler underTest = new AzureExceptionHandler();

    private static Stream<Arguments> concurrentWriteCheckTestSource() {
        return Stream.of(
                Arguments.of(401, "ConflictingConcurrentWriteNotAllowed", false),
                Arguments.of(409, "anything", false),
                Arguments.of(409, "ConflictingConcurrentWriteNotAllowed", true)
        );
    }

    private static Stream<Arguments> diskAlreadyAttachedCheckTestSource() {
        return Stream.of(
                Arguments.of(401, "ConflictingUserInput", "", false),
                Arguments.of(409, "anything", "", false),
                Arguments.of(409, "ConflictingUserInput", "", false),
                Arguments.of(409, "ConflictingUserInput", "cannot be attached as the disk is already owned by VM", false)
        );
    }

    @ParameterizedTest
    @MethodSource("concurrentWriteCheckTestSource")
    void concurrentWriteCheckTest(int statusCode, String azureErrorCode, boolean result) {
        ApiErrorException apiErrorException = getApiErrorException(statusCode, azureErrorCode, "");
        assertEquals(result, underTest.isConcurrentWrite(apiErrorException));
    }

    @ParameterizedTest
    @MethodSource("diskAlreadyAttachedCheckTestSource")
    void concurrentWriteCheckTest(int statusCode, String azureErrorCode, String azureErrorMessage, boolean result) {
        ApiErrorException apiErrorException = getApiErrorException(statusCode, azureErrorCode, azureErrorMessage);
        assertEquals(result, underTest.isConcurrentWrite(apiErrorException));
    }

    private ApiErrorException getApiErrorException(int statusCode, String azureErrorCode, String azureErrorMessage) {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(statusCode);
        ApiError apiError = mock(ApiError.class);
        lenient().when(apiError.getCode()).thenReturn(azureErrorCode);
        lenient().when(apiError.getMessage()).thenReturn(azureErrorMessage);
        return new ApiErrorException("", httpResponse, apiError);
    }

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
    void handleExceptionWhenManagementExceptionNotHandledThenThrowsCloudConnectorException() {
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
    void handleExceptionWithDefaultWhenManagementExceptionNotHandledThenThrowsCloudConnectorException() {
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
    void handleExceptionWithDefaultAndHandleNotFoundWhenOtherThanNotFoundThenThrowsCloudConnectorException(HttpStatus httpStatus) {
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

    @Test
    void testManagementExceptionIsConflict() {
        assertTrue(underTest.isExceptionCodeConflict(
                new ManagementException("asdf", mock(HttpResponse.class), new ManagementError("conflict", "asdffda"))));
        ManagementError managementErrorWithDetails = mock(ManagementError.class);
        when(managementErrorWithDetails.getCode()).thenReturn("Asdf");
        when(managementErrorWithDetails.getDetails()).thenAnswer(invocation -> List.of(new ManagementError("conflict", "asdffda")));
        assertTrue(underTest.isExceptionCodeConflict(
                new ManagementException("asdf", mock(HttpResponse.class), managementErrorWithDetails)));
        assertFalse(underTest.isExceptionCodeConflict(
                new ManagementException("asdf", mock(HttpResponse.class), new ManagementError("nope", "asdffda"))));
        assertFalse(underTest.isExceptionCodeConflict(
                new ManagementException("asdf", mock(HttpResponse.class), new ManagementError(null, "asdffda"))));
        assertFalse(underTest.isExceptionCodeConflict(
                new ManagementException("asdf", mock(HttpResponse.class))));
        assertFalse(underTest.isExceptionCodeConflict(null));
    }

}
