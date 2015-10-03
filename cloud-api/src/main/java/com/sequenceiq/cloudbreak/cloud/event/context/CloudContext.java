package com.sequenceiq.cloudbreak.cloud.event.context;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformVariant;

public class CloudContext {

    private final Long id;
    private final String name;
    private final String platform;
    private final String owner;
    private final String variant;

    public CloudContext(Long id, String name, String platform, String owner) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.owner = owner;
        this.variant = null;
    }

    public CloudContext(Long id, String name, String platform, String owner, String variant, Long created, String region) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.owner = owner;
        this.variant = variant;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPlatform() {
        return platform;
    }

    public String getVariant() {
        return variant;
    }

    public String getOwner() {
        return owner;
    }

    public CloudPlatformVariant getPlatformVariant() {
        return new CloudPlatformVariant(platform, variant);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudContext{");
        sb.append("id=").append(id);
        sb.append(", name='").append(name).append('\'');
        sb.append(", platform='").append(platform).append('\'');
        sb.append(", owner='").append(owner).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
