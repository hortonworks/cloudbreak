package com.sequenceiq.cloudbreak.common.type;

public enum DefaultApplicationTag {

    OWNER("Owner"),
    CB_USER_NAME("cb-user-name"),
    CB_VERSION("cb-version"),
    CB_ACOUNT_NAME("cb-account-name"),
    CB_RESOURCE_TYPE("cb-resource-type");

    private final String key;

    DefaultApplicationTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
