package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack;

import java.util.Optional;

public enum OpenstackSelector {

    DOMAIN("cb-keystone-v3-domain-scope"),
    PROJECT("cb-keystone-v3-project-scope");

    private String value;

    OpenstackSelector(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static Optional<OpenstackSelector> byValue(String value) {
        for (OpenstackSelector openstackSelector : OpenstackSelector.values()) {
            if (openstackSelector.getValue().equals(value)) {
                return Optional.of(openstackSelector);
            }
        }
        return Optional.empty();
    }

}
