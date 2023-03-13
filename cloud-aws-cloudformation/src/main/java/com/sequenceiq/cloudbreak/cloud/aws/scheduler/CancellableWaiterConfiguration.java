package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import software.amazon.awssdk.core.waiters.WaiterOverrideConfiguration;

public class CancellableWaiterConfiguration {

    private static final int DEFAULT_MAX_ATTEMPTS = 120;

    private CancellableWaiterConfiguration() {
    }

    public static WaiterOverrideConfiguration cancellableWaiterConfiguration(CancellationCheck cancellationCheck) {
        return WaiterOverrideConfiguration.builder()
                .backoffStrategy(new CancellableBackoffDelayStrategy(cancellationCheck))
                .maxAttempts(DEFAULT_MAX_ATTEMPTS)
                .build();
    }
}
