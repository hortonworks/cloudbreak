package com.sequenceiq.periscope.api.model;

import com.sequenceiq.periscope.doc.ApiDescription.ClusterAutoscaleState;
import com.sequenceiq.periscope.doc.ApiDescription.ClusterStopStartScalingState;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AutoscaleState")
public class AutoscaleClusterState {

    @Schema(description = ClusterAutoscaleState.ENABLE_AUTOSCALING)
    private Boolean enableAutoscaling;

    @Schema(description = ClusterStopStartScalingState.ENABLE_STOP_START_SCALING)
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
