package com.sequenceiq.freeipa.kerberosmgmt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtUtil;

public class KerberosMgmtUtilV1Test {
    private static final String MESSAGE = "exception message";

    private static final int NOT_FOUND = 4001;

    private static final int DUPLICATE_ENTRY = 4002;

    @Test
    public void testIsNotFoundException() throws Exception {
        Assertions.assertFalse(KerberosMgmtUtil.isNotFoundException(new FreeIpaClientException(MESSAGE)));
        Assertions.assertFalse(KerberosMgmtUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))));
        Assertions.assertTrue(KerberosMgmtUtil.isNotFoundException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

    @Test
    public void testIsDuplicateEntryException() throws Exception {
        Assertions.assertFalse(KerberosMgmtUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE)));
        Assertions.assertTrue(KerberosMgmtUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(DUPLICATE_ENTRY, MESSAGE, null))));
        Assertions.assertFalse(KerberosMgmtUtil.isDuplicateEntryException(new FreeIpaClientException(MESSAGE,
                new JsonRpcClientException(NOT_FOUND, MESSAGE, null))));
    }

}