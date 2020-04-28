package com.sequenceiq.freeipa.client;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

    @Test
    public void testIsDuplicateEntryException() throws Exception {
        Assertions.assertFalse(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE)));
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
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
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null)),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
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
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException(MESSAGE, new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY)));
        Assertions.assertTrue(FreeIpaClientExceptionUtil.isExceptionWithErrorCode(
                new FreeIpaClientException("", new FreeIpaClientException(MESSAGE,
                        new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))),
                Set.of(FreeIpaErrorCodes.DUPLICATE_ENTRY, FreeIpaErrorCodes.COMMAND_ERROR)));
    }

    @Test
    public void testConvertToRetryableIfNeeded() throws Exception {
        Assertions.assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new JsonRpcClientException(AUTHENTICATION_ERROR, MESSAGE, null)))
                instanceof RetryableFreeIpaClientException);
        Assertions.assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, new JsonRpcClientException(NOT_FOUND, MESSAGE, null)))
                instanceof RetryableFreeIpaClientException);
        Assertions.assertTrue(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, HttpStatus.UNAUTHORIZED.value()))
                instanceof RetryableFreeIpaClientException);
        Assertions.assertFalse(FreeIpaClientExceptionUtil.convertToRetryableIfNeeded(
                new FreeIpaClientException(MESSAGE, HttpStatus.OK.value()))
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

}