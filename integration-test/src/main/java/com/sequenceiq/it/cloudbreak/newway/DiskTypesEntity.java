package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;

public class DiskTypesEntity extends AbstractCloudbreakEntity<Integer, PlatformDisksJson> {
    public static final String DISKTYPES = "DISKTYPES";

    DiskTypesEntity(String newId) {
        super(newId);
        setRequest(1);
    }

    DiskTypesEntity() {
        this(DISKTYPES);
    }
}
