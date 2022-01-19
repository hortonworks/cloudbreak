package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.googlecode.jsonrpc4j.JsonRpcClientException;

public class FreeIpaClientExceptionUtilV1Test {
    private static final String MESSAGE = "exception message";

    private static final int AUTHENTICATION_ERROR = 1000;

    private static final int NOT_FOUND = 4001;

    private static final int DUPLICATE_ENTRY = 4002;

    @Test
    public void testIsNotFoundException() throws Exception {
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE)));
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))));
        assertTrue(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

    @Test
    public void testIsDuplicateEntryException() throws Exception {
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE)));
        assertTrue(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))));
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

    @Test
    public void testIsExceptionWithErrorCode() throws Exception {
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE,
                        new IllegalStateException(MESSAGE)),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(NOT_FOUND, MESSAGE, null)),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null)),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null)),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY, FreeIpaErrorCodes.COMMAND_ERROR)));
    }

    @Test
    public void testIsExceptionWithErrorCodeWrappedFreeIpaClientException() throws Exception {
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE,
                        new IllegalStateException(MESSAGE))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(NOT_FOUND, MESSAGE, null))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException("", new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY, FreeIpaErrorCodes.COMMAND_ERROR)));
    }

    @Test
    public void testConvertToRetryableIfNeeded() throws Exception {
        assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new JsonRpcClientException(AUTHENTICATION_ERROR, MESSAGE, null)))
                instanceof RetryableFreeIpaClientException);
        Assertions.assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new JsonRpcClientException(NOT_FOUND, MESSAGE, null)))
                instanceof RetryableFreeIpaClientException);
        assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, HttpStatus.UNAUTHORIZED.value()))
                instanceof RetryableFreeIpaClientException);
        Assertions.assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, HttpStatus.OK.value()))
                instanceof RetryableFreeIpaClientException);
        assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new IOException()))
                instanceof RetryableFreeIpaClientException);
        Assertions.assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new IllegalStateException()))
                instanceof RetryableFreeIpaClientException);
    }

    @Test
    public void testGetAncestorCauseBeforeFreeIpaClientExceptions() {
        Assertions.assertNull(FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(new FreeIpaClientException(MESSAGE)));
        Throwable ancestor = new IllegalStateException(MESSAGE);
        Assertions.assertEquals(ancestor,
                FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(new FreeIpaClientException(MESSAGE, ancestor)));
        Assertions.assertEquals(ancestor,
                FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(
                        new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, ancestor))));
        Assertions.assertEquals(ancestor,
                FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(
                        new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, ancestor)))));
    }

    @Test
    public void testIsExceptionWithIOExceptionCause() {
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(new FreeIpaClientException(MESSAGE, new IOException())));
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(new FreeIpaClientException(MESSAGE, new IllegalStateException())));
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, new IOException()))));
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, new IllegalStateException()))));
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE,
                        new RuntimeException("stream closed", new JsonParseException(mock(JsonParser.class), "stream closed"))))));
    }

    @Test
    public void testIgnoreNotFoundWithoutEx() throws FreeIpaClientException {
        FreeIpaClientExceptionUtil.ignoreNotFoundException(() -> {
        }, null);
    }

    @Test
    public void testIgnoreNotFoundWithNotFound() throws FreeIpaClientException {
        FreeIpaClientExceptionUtil.ignoreNotFoundException(
                () -> {
                    throw new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null));
                },
                null);
    }

    @Test
    public void testIgnoreNotFoundWithOtherEx() {
        Assertions.assertThrows(FreeIpaClientException.class, () -> FreeIpaClientExceptionUtil.ignoreNotFoundException(
                () -> {
                    throw new FreeIpaClientException("ERROR", new JsonRpcClientException(FreeIpaErrorCodes.COMMAND_ERROR.getValue(), "ERROR", null));
                },
                null));
    }

    @Test
    public void testIgnoreNotFoundWithValueWithoutEx() throws FreeIpaClientException {
        Optional<Object> result = FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(Object::new, null);

        assertTrue(result.isPresent());
    }

    @Test
    public void testIgnoreNotFoundWithValueWithNotFound() throws FreeIpaClientException {
        Optional<Object> result = FreeIpaClientExceptionUtil.ignoreNotFoundExceptionWithValue(
                () -> {
                    throw new FreeIpaClientException("Not found", new JsonRpcClientException(FreeIpaErrorCodes.NOT_FOUND.getValue(), "Not found", null));
                },
                null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIgnoreNotFoundWithValueWithOtherEx() {
        Assertions.assertThrows(FreeIpaClientException.class, () -> FreeIpaClientExceptionUtil.ignoreNotFoundException(
                () -> {
                    throw new FreeIpaClientException("ERROR", new JsonRpcClientException(FreeIpaErrorCodes.COMMAND_ERROR.getValue(), "ERROR", null));
                },
                null));
    }

}