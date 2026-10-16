package com.sequenceiq.cloudbreak.orchestrator.salt.client;

import java.util.OptionalInt;

public record SaltConnectorTimeouts(int connectTimeoutMs, OptionalInt readTimeoutMs, int proxyTimeoutMs) {

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 20_000;

    private static final int DEFAULT_PROXY_TIMEOUT_MS = 90_000;

    public static SaltConnectorTimeouts defaults() {
        return new SaltConnectorTimeouts(DEFAULT_CONNECT_TIMEOUT_MS, OptionalInt.empty(), DEFAULT_PROXY_TIMEOUT_MS);
    }

    public static SaltConnectorTimeouts of(int connectTimeoutMs, int readTimeoutMs) {
        return new SaltConnectorTimeouts(connectTimeoutMs, OptionalInt.of(readTimeoutMs), DEFAULT_PROXY_TIMEOUT_MS);
    }

    public static SaltConnectorTimeouts of(int connectTimeoutMs, int readTimeoutMs, int proxyTimeoutMs) {
        return new SaltConnectorTimeouts(connectTimeoutMs, OptionalInt.of(readTimeoutMs), proxyTimeoutMs);
    }
}
