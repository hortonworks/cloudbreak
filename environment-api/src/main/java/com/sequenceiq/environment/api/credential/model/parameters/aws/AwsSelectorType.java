package com.sequenceiq.environment.api.credential.model.parameters.aws;

public enum AwsSelectorType {

    ROLE_BASED("role-based"),
    KEY_BASED("key-based");

    private String name;

    AwsSelectorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
