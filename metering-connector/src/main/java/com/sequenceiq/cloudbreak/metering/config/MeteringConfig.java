package com.sequenceiq.cloudbreak.metering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("meteringingestion")
public class MeteringConfig {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 8982;

    private static final int DEFAULT_SYNC_INTERVAL_IN_SECONDS = 600;

    private static final int DEFAULT_SYNC_INITIAL_DELAY_IN_SECONDS = 180;

    private static final int DEFAULT_GRPC_TIMEOUT_IN_SECONDS = 10;

    private String host = DEFAULT_HOST;

    private int port = DEFAULT_PORT;

    private boolean enabled;

    private boolean instanceCheckerEnabled = true;

    private int syncIntervalInSeconds = DEFAULT_SYNC_INTERVAL_IN_SECONDS;

    private int syncInitialDelayInSeconds = DEFAULT_SYNC_INITIAL_DELAY_IN_SECONDS;

    private int grpcTimeoutInSeconds = DEFAULT_GRPC_TIMEOUT_IN_SECONDS;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInstanceCheckerEnabled() {
        return instanceCheckerEnabled;
    }

    public void setInstanceCheckerEnabled(boolean instanceCheckerEnabled) {
        this.instanceCheckerEnabled = instanceCheckerEnabled;
    }

    public int getSyncIntervalInSeconds() {
        return syncIntervalInSeconds;
    }

    public void setSyncIntervalInSeconds(int syncIntervalInSeconds) {
        this.syncIntervalInSeconds = syncIntervalInSeconds;
    }

    public int getSyncInitialDelayInSeconds() {
        return syncInitialDelayInSeconds;
    }

    public void setSyncInitialDelayInSeconds(int syncInitialDelayInSeconds) {
        this.syncInitialDelayInSeconds = syncInitialDelayInSeconds;
    }

    public int getGrpcTimeoutInSeconds() {
        return grpcTimeoutInSeconds;
    }

    public void setGrpcTimeoutInSeconds(int grpcTimeoutInSeconds) {
        this.grpcTimeoutInSeconds = grpcTimeoutInSeconds;
    }
}
