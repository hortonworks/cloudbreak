package com.sequenceiq.it.cloudbreak.newway;

import java.util.Collection;

import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;

public class DiskTypesEntity extends AbstractCloudbreakEntity<Integer, PlatformDisksJson, DiskTypesEntity> {
    public static final String DISKTYPES = "DISKTYPES";

    private String type;

    private Collection<String> responses;

    private final Integer request;

    private DiskTypesEntity(String newId) {
        super(newId);
        request = 1;
    }

    DiskTypesEntity() {
        this(DISKTYPES);
    }

    public Collection<String> getByFilterResponses() {
        return responses;
    }

    public void setByFilterResponses(Collection<String> responses) {
        this.responses = responses;
    }

    public String getType() {
        return type;
    }

    public DiskTypesEntity withType(String type) {
        this.type = type;
        return this;
    }

    public void setType(String type) {
        this.type = type;
    }
}
