package com.sequenceiq.cloudbreak.auth.altus;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;

public class RequestIdUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestIdUtil.class);

    private RequestIdUtil() {
    }

    public static String getOrGenerate(Optional<String> requestId) {
        return requestId.orElse(MDCBuilder.getOrGenerateRequestId());
    }

}
