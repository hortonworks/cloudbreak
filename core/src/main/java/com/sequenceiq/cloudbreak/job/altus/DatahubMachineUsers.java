package com.sequenceiq.cloudbreak.job.altus;

public class DatahubMachineUsers {

    private final String accountId;

    private final String fluentUser;

    private final String telemetryPublisherUser;

    public DatahubMachineUsers(String accountId, String fluentUser, String telemetryPublisherUser) {
        this.accountId = accountId;
        this.fluentUser = fluentUser;
        this.telemetryPublisherUser = telemetryPublisherUser;
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
}
