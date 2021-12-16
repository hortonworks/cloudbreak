package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterStopStartScalingState;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterAutoscaleState;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("AutoscaleState")
public class AutoscaleClusterState {

    @ApiModelProperty(ClusterAutoscaleState.ENABLE_AUTOSCALING)
    private Boolean enableAutoscaling;

    @ApiModelProperty(ClusterStopStartScalingState.ENABLE_STOP_START_SCALING)
    private Boolean useStopStartMechanism;

    public Boolean isEnableAutoscaling() {
        return enableAutoscaling;
    }

    public void setEnableAutoscaling(Boolean enableAutoscaling) {
        this.enableAutoscaling = enableAutoscaling;
    }

    public Boolean getUseStopStartMechanism() {
        return useStopStartMechanism;
    }

    public void setUseStopStartMechanism(Boolean useStopStartMechanism) {
        this.useStopStartMechanism = useStopStartMechanism;
    }

    public static AutoscaleClusterState enable() {
        AutoscaleClusterState stateJson = new AutoscaleClusterState();
        stateJson.enableAutoscaling = Boolean.TRUE;
        return stateJson;
    }

    public static AutoscaleClusterState disable() {
        AutoscaleClusterState stateJson = new AutoscaleClusterState();
        stateJson.enableAutoscaling = Boolean.FALSE;
        return stateJson;
    }

    public static AutoscaleClusterState enableStopStart() {
        AutoscaleClusterState stateJson = enable();
        stateJson.useStopStartMechanism = Boolean.TRUE;
        return stateJson;
    }

    public static AutoscaleClusterState disableStopStart() {
        AutoscaleClusterState stateJson = enable();
        stateJson.useStopStartMechanism = Boolean.FALSE;
        return stateJson;
    }
}
