package com.sequenceiq.cloudbreak.cloud.model.view;

import java.util.Map;

public class PlatformResourceSshKeyFilterView {

    private String keyName;

    public PlatformResourceSshKeyFilterView(Map<String, String> filters) {
        this.keyName = filters.get("keyName");
    }

    public String getKeyName() {
        return keyName;
    }
}
