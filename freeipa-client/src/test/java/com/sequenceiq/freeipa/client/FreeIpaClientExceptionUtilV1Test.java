package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

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
        assertFalse(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE)));
        assertFalse(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))));
        assertTrue(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

    @Test
    public void testIsDuplicateEntryException() throws Exception {
        assertFalse(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE)));
        assertTrue(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))));
        assertFalse(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

    @Test
    public void testIsExceptionWithErrorCode() throws Exception {
        assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE,
                        new IllegalStateException(MESSAGE)),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
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
        assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE,
                        new IllegalStateException(MESSAGE))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        assertFalse(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
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
        assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new JsonRpcClientException(NOT_FOUND, MESSAGE, null)))
                instanceof RetryableFreeIpaClientException);
        assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, HttpStatus.UNAUTHORIZED.value()))
                instanceof RetryableFreeIpaClientException);
        assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, HttpStatus.OK.value()))
                instanceof RetryableFreeIpaClientException);
        assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new IOException()))
                instanceof RetryableFreeIpaClientException);
        assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new IllegalStateException()))
                instanceof RetryableFreeIpaClientException);
    }

    @Test
    public void testGetAncestorCauseBeforeFreeIpaClientExceptions() {
        assertNull(FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(new FreeIpaClientException(MESSAGE)));
        Throwable ancestor = new IllegalStateException(MESSAGE);
        assertEquals(ancestor,
                FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(new FreeIpaClientException(MESSAGE, ancestor)));
        assertEquals(ancestor,
                FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(
                        new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, ancestor))));
        assertEquals(ancestor,
                FreeIpaClientExceptionUtil.getAncestorCauseBeforeFreeIpaClientExceptions(
                        new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, ancestor)))));
    }

    @Test
    public void testIsExceptionWithIOExceptionCause() {
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(new FreeIpaClientException(MESSAGE, new IOException())));
        assertFalse(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(new FreeIpaClientException(MESSAGE, new IllegalStateException())));
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, new IOException()))));
        assertFalse(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE, new IllegalStateException()))));
        assertTrue(FreeIpaClientExceptionUtil.isExceptionWithIOExceptionCause(
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
        assertThrows(FreeIpaClientException.class, () -> FreeIpaClientExceptionUtil.ignoreNotFoundException(
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
        assertThrows(FreeIpaClientException.class, () -> FreeIpaClientExceptionUtil.ignoreNotFoundException(
                () -> {
                    throw new FreeIpaClientException("ERROR", new JsonRpcClientException(FreeIpaErrorCodes.COMMAND_ERROR.getValue(), "ERROR", null));
                },
                null));
    }

}