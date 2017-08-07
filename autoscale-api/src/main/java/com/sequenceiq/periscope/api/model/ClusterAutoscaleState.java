package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("ClusterAutoscaleState")
public class ClusterAutoscaleState {

    @ApiModelProperty(ApiDescription.ClusterAutoscaleState.ENABLE_AUTOSCALING)
    private boolean enableAutoscaling;

    public boolean isEnableAutoscaling() {
        return enableAutoscaling;
    }

    public void setEnableAutoscaling(boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }
}
