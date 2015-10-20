package com.sequenceiq.cloudbreak.cloud.context;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Location;

public class CloudContext {

    private final Long id;
    private final String name;
    private final String platform;
    private final String owner;
    private final String variant;
    private final Location location;

    public CloudContext(Long id, String name, String platform, String owner) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.owner = owner;
        this.variant = null;
        this.location = null;
    }

    public CloudContext(Long id, String name, String platform, String owner, String variant, Location location) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.owner = owner;
        this.variant = variant;
        this.location = location;
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

    public Location getLocation() {
        return location;
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
