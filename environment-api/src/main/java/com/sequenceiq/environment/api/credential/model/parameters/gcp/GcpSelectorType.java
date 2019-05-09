package com.sequenceiq.environment.api.credential.model.parameters.gcp;

public enum GcpSelectorType {

    JSON("credential-json"),
    P12("credential-p12");

    private String name;

    GcpSelectorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
