package com.sequenceiq.freeipa.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Backup {
    private String storageLocation;

    private S3CloudStorageV1Parameters s3;

    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    private boolean monthlyFullEnabled;

    private boolean initialFullEnabled;

    private boolean hourlyEnabled;

    public String getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    public S3CloudStorageV1Parameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageV1Parameters s3) {
        this.s3 = s3;
    }

    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }

    public boolean isMonthlyFullEnabled() {
        return monthlyFullEnabled;
    }

    public void setMonthlyFullEnabled(boolean monthlyFullEnabled) {
        this.monthlyFullEnabled = monthlyFullEnabled;
    }

    public boolean isInitialFullEnabled() {
        return initialFullEnabled;
    }

    public void setInitialFullEnabled(boolean initialFullEnabled) {
        this.initialFullEnabled = initialFullEnabled;
    }

    public boolean isHourlyEnabled() {
        return hourlyEnabled;
    }

    public void setHourlyEnabled(boolean hourlyEnabled) {
        this.hourlyEnabled = hourlyEnabled;
    }
}
