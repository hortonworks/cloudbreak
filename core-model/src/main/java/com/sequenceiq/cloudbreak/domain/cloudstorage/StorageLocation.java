package com.sequenceiq.cloudbreak.domain.cloudstorage;

import com.sequenceiq.common.model.CloudStorageCdpService;

public class StorageLocation {

    private CloudStorageCdpService type;

    private String value;

    public CloudStorageCdpService getType() {
        return type;
    }

    public void setType(CloudStorageCdpService type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
