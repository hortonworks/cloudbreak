package com.sequenceiq.cloudbreak.cloud.aws.connector.resource.upgrade.operation;

public enum AwsRdsState {
    AVAILABLE("available"),

    UPGRADING("upgrading");

    private final String state;

    AwsRdsState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

}