package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.aws;

public enum AwsSelectorType {

    ROLE_BASED("role-based"),
    KEY_BASED("key-based");

    private final String name;

    AwsSelectorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
