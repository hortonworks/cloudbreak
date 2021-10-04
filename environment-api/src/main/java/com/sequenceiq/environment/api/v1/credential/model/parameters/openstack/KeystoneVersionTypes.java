package com.sequenceiq.environment.api.v1.credential.model.parameters.openstack;

@Deprecated
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
