package com.sequenceiq.cloudbreak.cloud.context;

import com.google.common.base.Strings;
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

    private final String accountUUID;

    private CloudContext(
            Long id,
            String name,
            String crn,
            String platform,
            String variant,
            Location location,
            String userId,
            String accountId,
            String userName,
            String accountUUID) {
        this.id = id;
        this.name = name;
        this.crn = crn;
        this.platform = Platform.platform(platform);
        this.variant = Variant.variant(variant);
        this.location = location;
        this.userId = userId;
        this.accountId = accountId;
        this.userName = userName;
        this.accountUUID = Strings.isNullOrEmpty(accountUUID) ? accountId : accountUUID;
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

    public String getAccountUUID() {
        return accountUUID;
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
        return "CloudContext{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", platform=" + platform +
                ", variant=" + variant +
                ", location=" + location +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", accountId='" + accountId + '\'' +
                ", crn='" + crn + '\'' +
                '}';
    }

    public static class Builder {
        private Long id;

        private String name;

        private String platform;

        private String variant;

        private Location location;

        private String userId;

        private String userName;

        private String accountId;

        private String accountUUID;

        private String crn;

        public Builder withId(Long id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withPlatform(String platform) {
            this.platform = platform;
            return this;
        }

        public Builder withVariant(String variant) {
            this.variant = variant;
            return this;
        }

        public Builder withLocation(Location location) {
            this.location = location;
            return this;
        }

        public Builder withUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder withUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withAccountId(Long accountId) {
            this.accountId = accountId == null ? null : accountId.toString();
            return this;
        }

        public Builder withWorkspaceId(Long workspaceId) {
            this.accountId = workspaceId.toString();
            return this;
        }

        public Builder withAccountUUID(String accountUUID) {
            this.accountUUID = accountUUID;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public static Builder builder() {
            return new Builder();
        }

        public CloudContext build() {
            return new CloudContext(
                    id,
                    name,
                    crn,
                    platform,
                    variant,
                    location,
                    userId,
                    accountId,
                    userName,
                    accountUUID
            );
        }
    }
}
