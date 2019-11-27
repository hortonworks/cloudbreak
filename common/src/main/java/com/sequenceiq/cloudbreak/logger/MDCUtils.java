package com.sequenceiq.cloudbreak.logger;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

public class MDCUtils {

    private MDCUtils() {

    }

    public static Optional<String> getRequestId() {
        String requestId = MDC.get(LoggerContextKey.REQUEST_ID.toString());
        if (!StringUtils.isEmpty(requestId)) {
            return Optional.of(requestId);
        }
        return Optional.empty();
    }
}
