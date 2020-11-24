package com.sequenceiq.cloudbreak.auth.altus;

import java.util.Optional;
import java.util.UUID;

public class RequestIdUtil {

    private RequestIdUtil() {
    }

    public static String getOrGenerate(Optional<String> requestId) {
        return requestId.orElseGet(RequestIdUtil::newRequestId);
    }

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }
}
