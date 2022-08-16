package com.sequenceiq.cloudbreak.job.altus;

public class DatahubMachineUsers {

    private final String accountId;

    private final String fluentUser;

    private final String telemetryPublisherUser;

    private final String monitoringUser;

    public DatahubMachineUsers(String accountId, String fluentUser, String telemetryPublisherUser, String monitoringUser) {
        this.accountId = accountId;
        this.fluentUser = fluentUser;
        this.telemetryPublisherUser = telemetryPublisherUser;
        this.monitoringUser = monitoringUser;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getFluentUser() {
        return fluentUser;
    }

    public String getTelemetryPublisherUser() {
        return telemetryPublisherUser;
    }

    public String getMonitoringUser() {
        return monitoringUser;
    }
}
