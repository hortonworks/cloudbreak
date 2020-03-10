package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterAutoscaleState;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AutoscaleState")
public class AutoscaleClusterState {

    @ApiModelProperty(ClusterAutoscaleState.ENABLE_AUTOSCALING)
    private boolean enableAutoscaling;

    public boolean isEnableAutoscaling() {
        return enableAutoscaling;
    }

    public void setEnableAutoscaling(boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public static AutoscaleClusterState enable() {
        AutoscaleClusterState stateJson = new AutoscaleClusterState();
        stateJson.enableAutoscaling = true;
        return stateJson;
    }

    public static AutoscaleClusterState disable() {
        AutoscaleClusterState stateJson = new AutoscaleClusterState();
        stateJson.enableAutoscaling = false;
        return stateJson;
    }
}
