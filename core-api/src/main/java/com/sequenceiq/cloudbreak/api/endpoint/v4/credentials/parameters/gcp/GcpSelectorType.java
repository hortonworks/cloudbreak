package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.gcp;

public enum GcpSelectorType {

    JSON("credential-json"),
    P12("credential-p12");

    String name;

    GcpSelectorType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
