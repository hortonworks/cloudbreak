package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.S3CloudStorageV1Parameters;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Logging implements Serializable {

    private String storageLocation;

    private S3CloudStorageV1Parameters s3;

    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    private GcsCloudStorageV1Parameters gcs;

    @Deprecated
    private CloudwatchParams cloudwatch;

    private Set<SensitiveLoggingComponent> enabledSensitiveStorageLogs;

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

    public GcsCloudStorageV1Parameters getGcs() {
        return gcs;
    }

    public void setGcs(GcsCloudStorageV1Parameters gcs) {
        this.gcs = gcs;
    }

    @Deprecated
    public CloudwatchParams getCloudwatch() {
        return cloudwatch;
    }

    @Deprecated
    public void setCloudwatch(CloudwatchParams cloudwatch) {
        this.cloudwatch = cloudwatch;
    }

    public Set<SensitiveLoggingComponent> getEnabledSensitiveStorageLogs() {
        return enabledSensitiveStorageLogs;
    }

    public void setEnabledSensitiveStorageLogs(Set<SensitiveLoggingComponent> enabledSensitiveStorageLogs) {
        this.enabledSensitiveStorageLogs = enabledSensitiveStorageLogs;
    }

    public void setEnabledSensitiveStorageLogsByStrings(Set<String> enabledSensitiveStorageLogs) {
        if (enabledSensitiveStorageLogs != null) {
            this.enabledSensitiveStorageLogs = enabledSensitiveStorageLogs.stream()
                    .filter(stringValue -> Arrays.stream(SensitiveLoggingComponent.values())
                            .anyMatch(enumValue -> StringUtils.equals(enumValue.name(), stringValue)))
                    .map(SensitiveLoggingComponent::valueOf)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Logging.class.getSimpleName() + "[", "]")
                .add("storageLocation='" + storageLocation + '\'')
                .add("s3='" + s3 + '\'')
                .add("adlsGen2='" + adlsGen2 + '\'')
                .add("gcs='" + gcs + '\'')
                .add("cloudwatch='" + cloudwatch + '\'')
                .add("enabledSensitiveStorageLogs='" + enabledSensitiveStorageLogs + '\'')
                .toString();
    }
}
