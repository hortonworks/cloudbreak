package com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.parameters.openstack;

import java.util.Optional;

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

    public static Optional<KeystoneVersionTypes> byType(String type) {
        for (KeystoneVersionTypes value : KeystoneVersionTypes.values()) {
            if (value.getType().equals(type)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

}
