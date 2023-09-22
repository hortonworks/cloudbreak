package com.sequenceiq.cloudbreak.metering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("meteringingestion")
public class MeteringConfig {

    private static final String DEFAULT_HOST = "localhost";

    private static final int DEFAULT_PORT = 8982;

    private static final int DEFAULT_SYNC_INTERVAL_IN_SECONDS = 600;

    private static final int DEFAULT_INSTANCE_CHECKER_INTERVAL_IN_HOURS = 6;

    private static final int DEFAULT_INSTANCE_CHECKER_DELAY_IN_SECONDS = 7200;

    private static final int DEFAULT_GRPC_TIMEOUT_IN_SECONDS = 10;

    private String host = DEFAULT_HOST;

    private int port = DEFAULT_PORT;

    private boolean enabled;

    private boolean instanceCheckerEnabled = true;

    private int syncIntervalInSeconds = DEFAULT_SYNC_INTERVAL_IN_SECONDS;

    private int instanceCheckerIntervalInHours = DEFAULT_INSTANCE_CHECKER_INTERVAL_IN_HOURS;

    private int instanceCheckerDelayInSeconds = DEFAULT_INSTANCE_CHECKER_DELAY_IN_SECONDS;

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

    public int getInstanceCheckerIntervalInHours() {
        return instanceCheckerIntervalInHours;
    }

    public void setInstanceCheckerIntervalInHours(int instanceCheckerIntervalInHours) {
        this.instanceCheckerIntervalInHours = instanceCheckerIntervalInHours;
    }

    public int getInstanceCheckerDelayInSeconds() {
        return instanceCheckerDelayInSeconds;
    }

    public void setInstanceCheckerDelayInSeconds(int instanceCheckerDelayInSeconds) {
        this.instanceCheckerDelayInSeconds = instanceCheckerDelayInSeconds;
    }

    public int getGrpcTimeoutInSeconds() {
        return grpcTimeoutInSeconds;
    }

    public void setGrpcTimeoutInSeconds(int grpcTimeoutInSeconds) {
        this.grpcTimeoutInSeconds = grpcTimeoutInSeconds;
    }
}
