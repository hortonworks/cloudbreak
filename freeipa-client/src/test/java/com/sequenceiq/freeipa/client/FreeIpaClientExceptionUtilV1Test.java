package com.sequenceiq.freeipa.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.googlecode.jsonrpc4j.JsonRpcClientException;

public class FreeIpaClientExceptionUtilV1Test {
    private static final String MESSAGE = "exception message";

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

}