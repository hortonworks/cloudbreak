package com.sequenceiq.cloudbreak.tag;

import java.util.HashSet;
import java.util.Set;

public enum HandleBarModelKey {

    CLOUD_PLATFORM("cloudPlatform"),
    USER_NAME("userName"),
    USER_CRN("userCrn"),
    CREATOR_CRN("creatorCrn"),
    TIME("time"),
    ACCOUNT_ID("accountId"),
    RESOURCE_CRN("resourceCrn");

    private final String modelKey;

    HandleBarModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public String modelKey() {
        return modelKey;
    }

    public static Set<String> modelKeys() {
        Set<String> result = new HashSet<>();
        for (HandleBarModelKey handleBarModelKey : HandleBarModelKey.values()) {
            result.add(handleBarModelKey.modelKey());
        }
        return result;
    }

}
