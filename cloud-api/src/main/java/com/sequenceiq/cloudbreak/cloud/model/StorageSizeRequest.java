package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Date;

import javax.validation.constraints.NotNull;

public class StorageSizeRequest {

    private @NotNull CloudCredential credential;

    private @NotNull String objectStoragePath;

    private Region region;

    private Date startTime;

    private Date endTime;

    public StorageSizeRequest() {
    }

    public StorageSizeRequest(Builder builder) {
        this.credential = builder.credential;
        this.objectStoragePath = builder.objectStoragePath;
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

    public String getObjectStoragePath() {
        return objectStoragePath;
    }

    public void setObjectStoragePath(String objectStoragePath) {
        this.objectStoragePath = objectStoragePath;
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
                ", objectStoragePath='" + objectStoragePath + '\'' +
                ", region=" + region +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }

    public static class Builder {

        private CloudCredential credential;

        private String objectStoragePath;

        private Region region;

        private Date startTime;

        private Date endTime;

        public Builder withCredential(CloudCredential credential) {
            this.credential = credential;
            return this;
        }

        public Builder withObjectStoragePath(String objectStoragePath) {
            this.objectStoragePath = objectStoragePath;
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
