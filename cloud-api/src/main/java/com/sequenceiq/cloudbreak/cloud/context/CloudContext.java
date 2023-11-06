package com.sequenceiq.cloudbreak.cloud.context;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;

/**
 * Context object is used to identify messages exchanged between core and Cloud Platform. This context object passed along
 * with the flow to all methods and also sent back in the Response objects.
 *
 */
@JsonDeserialize(builder = CloudContext.Builder.class)
public class CloudContext {

    private final Long id;

    private final String name;

    private final Platform platform;

    private final boolean govCloud;

    private final Variant variant;

    private final Location location;

    private final String userName;

    private final String accountId;

    /**
     * This field is the result of a misconception in the code by using the TenantId (a sequence) as the AccountId (UUID)
     * To keep already generated items with using the TenantId sequence backward compatible, this field must not be removed.
     */
    private final Long tenantId;

    private final String crn;

    /**
     * Specifically used for cases in which the original stack has been detached during a resize operation.
     */
    private final String originalName;

    /*
     * We need this constructor because flow request objects use this class and it can not be serialized back to object
     */
    private CloudContext(Long id, String name, Platform platform, boolean govCloud, Variant variant, Location location, String userName, String accountId,
            Long tenantId, String crn, String originalName) {
        this.id = id;
        this.name = name;
        this.platform = platform;
        this.govCloud = govCloud;
        this.variant = variant;
        this.location = location;
        this.userName = userName;
        this.accountId = accountId;
        this.tenantId = tenantId;
        this.crn = crn;
        this.originalName = originalName;
    }

    private CloudContext(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.crn = builder.crn;
        this.platform = Platform.platform(builder.platform);
        this.variant = Variant.variant(builder.variant);
        this.location = builder.location;
        this.accountId = builder.accountId;
        this.tenantId = builder.tenantId;
        this.userName = builder.userName;
        this.govCloud = builder.govCloud;
        this.originalName = builder.originalName;
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

    public String getAccountId() {
        return accountId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getUserName() {
        return userName;
    }

    public String getCrn() {
        return crn;
    }

    public boolean isGovCloud() {
        return govCloud;
    }

    public String getOriginalName() {
        return originalName;
    }

    public Builder createPrototype() {
        return CloudContext.Builder.builder()
                .withId(getId())
                .withName(getName())
                .withPlatform(getPlatform())
                .withVariant(getVariant())
                .withLocation(getLocation())
                .withAccountId(getAccountId())
                .withTenantId(getTenantId())
                .withUserName(getUserName())
                .withCrn(getCrn())
                .withGovCloud(isGovCloud())
                .withOriginalName(getOriginalName());
    }

    @Override
    public String toString() {
        return "CloudContext{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", platform=" + platform +
                ", variant=" + variant +
                ", location=" + location +
                ", userName='" + userName + '\'' +
                ", accountId='" + accountId + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", crn='" + crn + '\'' +
                ", govCloud='" + govCloud + '\'' +
                '}';
    }

    @JsonPOJOBuilder
    public static class Builder {
        private Long id;

        private String name;

        private String platform;

        private String variant;

        private Location location;

        private String userName;

        private String accountId;

        private Long tenantId;

        private String crn;

        private boolean govCloud;

        private String originalName;

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

        @JsonProperty("platform")
        public Builder withPlatform(Platform platform) {
            this.platform = platform.getValue();
            return this;
        }

        public Builder withVariant(String variant) {
            this.variant = variant;
            return this;
        }

        @JsonProperty("variant")
        public Builder withVariant(Variant variant) {
            this.variant = variant.getValue();
            return this;
        }

        public Builder withLocation(Location location) {
            this.location = location;
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

        public Builder withTenantId(Long tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder withWorkspaceId(Long workspaceId) {
            this.accountId = workspaceId.toString();
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withGovCloud(boolean govCloud) {
            this.govCloud = govCloud;
            return this;
        }

        public Builder withOriginalName(String originalName) {
            this.originalName = originalName;
            return this;
        }

        public static Builder builder() {
            return new Builder();
        }

        public CloudContext build() {
            return new CloudContext(this);
        }
    }
}
