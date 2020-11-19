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

    private final String userName;

    private final String accountId;

    private final String crn;

    public CloudContext(Long id, String name, String crn, String platform, String userId, String accountId) {
        this.id = id;
        this.name = name;
        this.crn = crn;
        this.platform = Platform.platform(platform);
        this.userId = userId;
        this.accountId = accountId;
        variant = null;
        location = null;
        userName = null;
    }

    public CloudContext(Long id, String name, String crn, String platform, String userId, Long workspaceId) {
        this(id, name, crn, platform, userId, workspaceId.toString());
    }

    public CloudContext(Long id, String name, String crn, String platform, String variant,
            Location location, String userId, String accountId) {
        this.id = id;
        this.name = name;
        this.crn = crn;
        this.platform = Platform.platform(platform);
        this.variant = Variant.variant(variant);
        this.location = location;
        this.userId = userId;
        this.accountId = accountId;
        userName = null;
    }

    public CloudContext(Long id, String name, String crn, String platform, String variant,
                        Location location, String userId, Long workspaceId) {
        this(id, name, crn, platform, variant, location, userId, workspaceId.toString());
    }

    public CloudContext(Long id, String name, String crn, String platform, String variant,
            Location location, String userId, String userName, String accountId) {
        this.id = id;
        this.name = name;
        this.crn = crn;
        this.platform = Platform.platform(platform);
        this.variant = Variant.variant(variant);
        this.location = location;
        this.userId = userId;
        this.accountId = accountId;
        this.userName = userName;
    }

    public CloudContext(Long id, String name, String crn, String platform, String variant,
                        Location location, String userId, String userName, Long workspaceId) {
        this(id, name, crn, platform, variant, location, userId, userName, workspaceId.toString());
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

    public String getAccountId() {
        return accountId;
    }

    public String getUserName() {
        return userName;
    }

    public String getCrn() {
        return crn;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudContext{")
        .append("id=").append(id)
        .append(", name='").append(name).append('\'')
        .append(", platform='").append(platform).append('\'')
        .append(", userId='").append(userId).append('\'')
        .append(", workspaceId='").append(accountId).append('\'')
        .append(", location='").append(location).append('\'')
        .append('}');
        return sb.toString();
    }
}
