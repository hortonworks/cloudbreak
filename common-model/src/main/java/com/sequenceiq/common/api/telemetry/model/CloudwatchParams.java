package com.sequenceiq.common.api.telemetry.model;

import java.io.Serializable;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.common.api.telemetry.doc.TelemetryModelDescription;

import io.swagger.annotations.ApiModelProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CloudwatchParams implements Serializable {

    @ApiModelProperty
    @NotNull
    private String instanceProfile;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_CLOUDWATCH_PARAMS_REGION)
    private String region;

    @ApiModelProperty(TelemetryModelDescription.TELEMETRY_CLOUDWATCH_PARAMS)
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
        CloudwatchParams newCloudwatchParams = null;
        if (cloudwatchParams != null) {
            newCloudwatchParams = new CloudwatchParams();
            newCloudwatchParams.setStreamKey(cloudwatchParams.getStreamKey());
            newCloudwatchParams.setInstanceProfile(cloudwatchParams.getInstanceProfile());
            newCloudwatchParams.setRegion(cloudwatchParams.getRegion());
        }
        return newCloudwatchParams;
    }
}
