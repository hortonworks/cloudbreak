package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.constraints.NotNull;

public class StorageSizeRequest {

    private @NotNull CloudCredential credential;

    private @NotNull Set<String> cloudObjectIds;

    private Region region;

    private Date startTime;

    private Date endTime;

    public StorageSizeRequest() {
    }

    public StorageSizeRequest(Builder builder) {
        this.credential = builder.credential;
        this.cloudObjectIds = builder.cloudObjectIds;
        this.region = builder.region;
        this.startTime = builder.startTime;
        this.endTime = builder.endTime;
    }

    public CloudCredential getCredential() {
        return credential;
    }

    public void setCredential(CloudCredential credential) {
        this.credential = credential;
    }

    public Set<String> getCloudObjectIds() {
        return cloudObjectIds;
    }

    public String getCloudObjectIdsString() {
        return cloudObjectIds.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    public String getFirstCloudObjectId() {
        return cloudObjectIds.stream().findFirst().orElse(null);
    }

    public void setCloudObjectIds(Set<String> cloudObjectIds) {
        this.cloudObjectIds = cloudObjectIds;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ObjectStorageSizeRequest{" +
                "credential=" + credential +
                ", cloudObjectIds='" + cloudObjectIds + '\'' +
                ", region=" + region +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }

    public static class Builder {

        private CloudCredential credential;

        private Set<String> cloudObjectIds;

        private Region region;

        private Date startTime;

        private Date endTime;

        public Builder withCredential(CloudCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withCloudObjectIds(Set<String> cloudObjectIds) {
            this.cloudObjectIds = cloudObjectIds;
            return this;
        }

        public Builder withRegion(Region region) {
            this.region = region;
            return this;
        }

        public Builder withStartTime(Date startTime) {
            this.startTime = startTime;
            return this;
        }

        public Builder withEndTime(Date endTime) {
            this.endTime = endTime;
            return this;
        }

        public StorageSizeRequest build() {
            return new StorageSizeRequest(this);
        }
    }
}
