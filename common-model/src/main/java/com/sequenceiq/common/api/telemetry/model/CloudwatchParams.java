package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.v3.oas.annotations.media.Schema;

@Deprecated
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudwatchParams implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String instanceProfile;

    @Schema(description = TelemetryModelDescription.TELEMETRY_CLOUDWATCH_PARAMS_REGION)
    private String region;

    @Schema(description = TelemetryModelDescription.TELEMETRY_CLOUDWATCH_PARAMS)
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
    public static CloudwatchParams copy(CloudwatchParams cloudwatchParams) {
        if (cloudwatchParams == null) {
            return null;
        }
        CloudwatchParams newCloudwatchParams = new CloudwatchParams();
        newCloudwatchParams.setStreamKey(cloudwatchParams.getStreamKey());
        newCloudwatchParams.setInstanceProfile(cloudwatchParams.getInstanceProfile());
        newCloudwatchParams.setRegion(cloudwatchParams.getRegion());
        return newCloudwatchParams;
    }

    @Override
    public String toString() {
        return "CloudwatchParams{" +
                "instanceProfile='" + instanceProfile + '\'' +
                ", region='" + region + '\'' +
                ", streamKey=" + streamKey +
                '}';
    }
}
