package com.sequenceiq.periscope.api.model;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterJsonsProperties;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class DistroXAutoscaleClusterRequest implements Json {
    @ApiModelProperty(ClusterJsonsProperties.ENABLE_AUTOSCALING)
    private @NotNull Boolean enableAutoscaling;

    @ApiModelProperty(ClusterJsonsProperties.TIME_ALERTS)
    private List<@Valid TimeAlertRequest> timeAlertRequests;

    @ApiModelProperty(ClusterJsonsProperties.LOAD_ALERTS)
    private List<@Valid LoadAlertRequest> loadAlertRequests;

    public DistroXAutoscaleClusterRequest() {
    }

    public void setEnableAutoscaling(Boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public List<TimeAlertRequest> getTimeAlertRequests() {
        return timeAlertRequests != null ? timeAlertRequests : List.of();
    }

    public void setTimeAlertRequests(List<TimeAlertRequest> timeAlertRequests) {
        this.timeAlertRequests = timeAlertRequests;
    }

    public Boolean getEnableAutoscaling() {
        return enableAutoscaling;
    }

    public List<LoadAlertRequest> getLoadAlertRequests() {
        return loadAlertRequests != null ? loadAlertRequests : List.of();
    }

    public void setLoadAlertRequests(List<LoadAlertRequest> loadAlertRequests) {
        this.loadAlertRequests = loadAlertRequests;
    }
}
