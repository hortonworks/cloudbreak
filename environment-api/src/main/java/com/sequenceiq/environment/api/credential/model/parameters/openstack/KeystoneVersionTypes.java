package com.sequenceiq.environment.api.credential.model.parameters.openstack;

public enum KeystoneVersionTypes {

    V2("cb-keystone-v2"),
    V3("cb-keystone-v3");

    private String type;

    KeystoneVersionTypes(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

}
