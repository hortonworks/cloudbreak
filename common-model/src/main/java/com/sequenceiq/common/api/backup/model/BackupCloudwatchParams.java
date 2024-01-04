package com.sequenceiq.common.api.backup.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.backup.doc.BackupModelDescription;
import com.sequenceiq.common.api.telemetry.model.CloudwatchStreamKey;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BackupCloudwatchParams implements Serializable {

    @Schema
    @NotNull
    private String instanceProfile;

    @Schema(description = BackupModelDescription.CLOUDWATCH_PARAMS_REGION)
    private String region;

    @Schema(description = BackupModelDescription.CLOUDWATCH_PARAMS)
    private CloudwatchStreamKey streamKey = CloudwatchStreamKey.HOSTNAME;

    public String getInstanceProfile() {
        return instanceProfile;
    }

    public void setInstanceProfile(String instanceProfile) {
        this.instanceProfile = instanceProfile;
    }

    public CloudwatchStreamKey getStreamKey() {
        return streamKey;
    }

    public void setStreamKey(CloudwatchStreamKey streamKey) {
        this.streamKey = streamKey;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @JsonIgnore
    public static BackupCloudwatchParams copy(BackupCloudwatchParams cloudwatchParams) {
        if (cloudwatchParams == null) {
            return null;
        }
        BackupCloudwatchParams newCloudwatchParams = new BackupCloudwatchParams();
        newCloudwatchParams.setStreamKey(cloudwatchParams.getStreamKey());
        newCloudwatchParams.setInstanceProfile(cloudwatchParams.getInstanceProfile());
        newCloudwatchParams.setRegion(cloudwatchParams.getRegion());
        return newCloudwatchParams;
    }

    @Override
    public String toString() {
        return "BackupCloudwatchParams{" +
                "instanceProfile='" + instanceProfile + '\'' +
                ", region='" + region + '\'' +
                ", streamKey=" + streamKey +
                '}';
    }
}
