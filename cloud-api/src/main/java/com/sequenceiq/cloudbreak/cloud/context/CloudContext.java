package com.sequenceiq.cloudbreak.cloud.context;

import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

/**
 * Context object is used to identify messages exchanged between core and Cloud Platfrom. This context object passed along
 * with the flow to all methods and also sent back in the Response objects.
 *
 */
public class CloudContext {

    private final Long id;

    private final String name;

    private final Platform platform;

    private final Variant variant;

    private final Location location;

    private final String userId;

    private final Long workspaceId;

    public CloudContext(Long id, String name, String platform, String userId, Long workspaceId) {
        this.id = id;
        this.name = name;
        this.platform = Platform.platform(platform);
        this.userId = userId;
        this.workspaceId = workspaceId;
        variant = null;
        location = null;
    }

    public CloudContext(Long id, String name, String platform, String variant,
            Location location, String userId, Long workspaceId) {
        this.id = id;
        this.name = name;
        this.platform = Platform.platform(platform);
        this.variant = Variant.variant(variant);
        this.location = location;
        this.userId = userId;
        this.workspaceId = workspaceId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Variant getVariant() {
        return variant;
    }

    public CloudPlatformVariant getPlatformVariant() {
        return new CloudPlatformVariant(platform, variant);
    }

    public Location getLocation() {
        return location;
    }

    public String getUserId() {
        return userId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudContext{")
        .append("id=").append(id)
        .append(", name='").append(name).append('\'')
        .append(", platform='").append(platform).append('\'')
        .append(", userId='").append(userId).append('\'')
        .append(", workspaceId='").append(workspaceId).append('\'')
        .append('}');
        return sb.toString();
    }
}
