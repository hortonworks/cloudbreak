package com.sequenceiq.freeipa.kerberosmgmt.v1;

import java.util.Optional;

import com.googlecode.jsonrpc4j.JsonRpcClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientException;

public class KerberosMgmtUtil {

    private static final int NOT_FOUND_ERROR_CODE = 4001;

    private static final int DUPLICATE_ENTRY_ERROR_CODE = 4002;

    private KerberosMgmtUtil() {
    }

    public static boolean isNotFoundException(FreeIpaClientException e) {
        return Optional.ofNullable(e.getCause())
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .filter(c -> c == NOT_FOUND_ERROR_CODE)
                .isPresent();
    }

    public static boolean isDuplicateEntryException(FreeIpaClientException e) {
        return Optional.ofNullable(e.getCause())
                .filter(JsonRpcClientException.class::isInstance)
                .map(JsonRpcClientException.class::cast)
                .map(JsonRpcClientException::getCode)
                .filter(c -> c == DUPLICATE_ENTRY_ERROR_CODE)
                .isPresent();
    }
}
