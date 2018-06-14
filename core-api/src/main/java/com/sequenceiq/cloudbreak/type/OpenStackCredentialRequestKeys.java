package com.sequenceiq.cloudbreak.type;

public enum OpenStackCredentialRequestKeys {

    VERSION("keystoneVersion"),
    SCOPE("keystoneAuthScope");

    private final String value;

    OpenStackCredentialRequestKeys(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
