package com.sequenceiq.cloudbreak.common.type;

public enum DefaultApplicationTag {

    OWNER("Owner"),
    CDP_USER_NAME("cdp-user-name"),
    CDP_CB_VERSION("cdp-cb-version"),
    CDP_ACOUNT_NAME("cdp-account-name"),
    CDP_RESOURCE_TYPE("cdp-resource-type"),
    CDP_CREATION_TIMESTAMP("cdp-creation-timestamp"),
    CDP_CREATION_DATETIME_UTC("cdp-creation-datetime-utc");

    private final String key;

    DefaultApplicationTag(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
