package com.sequenceiq.environment.api.credential.model.parameters.openstack;

public enum KeystoneAuthScopes {

    DOMAIN("cb-keystone-v3-domain-scope"),
    PROJECT("cb-keystone-v3-project-scope");

    private String value;

    KeystoneAuthScopes(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
