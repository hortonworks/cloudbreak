package com.sequenceiq.cloudbreak.logger;

import java.util.Optional;

public class MDCUtils {

    private MDCUtils() {

    }

    public static Optional<String> getRequestId() {
        return Optional.of(MDCBuilder.getOrGenerateRequestId());
    }
}
