package com.sequenceiq.environment.environment.dto.telemetry;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.cloudstorage.old.AdlsGen2CloudStorageV1Parameters;
import com.sequenceiq.common.api.cloudstorage.old.GcsCloudStorageV1Parameters;
import com.sequenceiq.common.api.telemetry.model.CloudwatchParams;
import com.sequenceiq.common.api.telemetry.model.SensitiveLoggingComponent;
import com.sequenceiq.environment.environment.dto.StorageLocationAware;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnvironmentLogging implements Serializable, StorageLocationAware {

    private String storageLocation;

    private S3CloudStorageParameters s3;

    private AdlsGen2CloudStorageV1Parameters adlsGen2;

    private GcsCloudStorageV1Parameters gcs;

    @Deprecated
    private CloudwatchParams cloudwatch;

    private Set<String> enabledSensitiveStorageLogs;

    public String getStorageLocation() {
        return storageLocation;
    }

    @Override
    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }

    @Override
    public S3CloudStorageParameters getS3() {
        return s3;
    }

    public void setS3(S3CloudStorageParameters s3) {
        this.s3 = s3;
    }

    @Override
    public AdlsGen2CloudStorageV1Parameters getAdlsGen2() {
        return adlsGen2;
    }

    public void setAdlsGen2(AdlsGen2CloudStorageV1Parameters adlsGen2) {
        this.adlsGen2 = adlsGen2;
    }

    @Override
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

    public Set<String> getEnabledSensitiveStorageLogs() {
        return enabledSensitiveStorageLogs;
    }

    public void setEnabledSensitiveStorageLogs(Set<String> enabledSensitiveStorageLogs) {
        this.enabledSensitiveStorageLogs = enabledSensitiveStorageLogs;
    }

    public void setEnabledSensitiveStorageLogsByEnum(Set<SensitiveLoggingComponent> enabledSensitiveStorageLogs) {
        this.enabledSensitiveStorageLogs =
                CollectionUtils.emptyIfNull(enabledSensitiveStorageLogs).stream().map(SensitiveLoggingComponent::name).collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "EnvironmentLogging{" +
                "storageLocation='" + storageLocation + '\'' +
                ", s3=" + s3 +
                ", adlsGen2=" + adlsGen2 +
                ", gcs=" + gcs +
                ", cloudwatch=" + cloudwatch +
                ", enabledSensitiveStorageLogs=" + enabledSensitiveStorageLogs +
                '}';
    }

}
