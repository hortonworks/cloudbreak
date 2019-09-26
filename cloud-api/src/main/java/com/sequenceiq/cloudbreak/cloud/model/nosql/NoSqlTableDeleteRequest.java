package com.sequenceiq.cloudbreak.cloud.model.nosql;

import java.util.Objects;

import javax.validation.constraints.NotNull;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.base.RegionAndCredentialAwareRequestBase;

public class NoSqlTableDeleteRequest extends RegionAndCredentialAwareRequestBase {

    private @NotNull CloudCredential credential;

    private @NotNull String region;

    private @NotNull String cloudPlatform;

    private @NotNull String tableName;

    public NoSqlTableDeleteRequest() {
    }

    public NoSqlTableDeleteRequest(Builder builder) {
        this.credential = builder.credential;
        this.region = builder.region;
        this.cloudPlatform = builder.cloudPlatform;
        this.tableName = builder.tableName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public CloudCredential getCredential() {
        return credential;
    }

    public void setCredential(CloudCredential credential) {
        this.credential = credential;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        NoSqlTableDeleteRequest request = (NoSqlTableDeleteRequest) o;
        return Objects.equals(credential, request.credential) &&
                Objects.equals(region, request.region) &&
                Objects.equals(cloudPlatform, request.cloudPlatform) &&
                Objects.equals(tableName, request.tableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(credential, region, cloudPlatform, tableName);
    }

    @Override
    public String toString() {
        return "NoSqlTableDeleteRequest{" +
                "credential=" + credential +
                ", region='" + region + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", tableName='" + tableName + '\'' +
                '}';
    }

    @Override
    public Platform platform() {
        return Platform.platform(cloudPlatform);
    }

    @Override
    public Variant variant() {
        return Variant.variant(cloudPlatform);
    }

    public static class Builder {

        private CloudCredential credential;

        private String region;

        private String cloudPlatform;

        private String tableName;

        public Builder withCredential(CloudCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withRegion(String region) {
            this.region = region;
            return this;
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withTableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public NoSqlTableDeleteRequest build() {
            return new NoSqlTableDeleteRequest(this);
        }
    }
}
