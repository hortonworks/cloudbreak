package com.sequenceiq.environment.api.credential.model.parameters.openstack;

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

}
