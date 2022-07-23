package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.common.type.ClusterManagerType;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.dto.NetworkScaleDetails;
import com.sequenceiq.common.api.adjustment.AdjustmentTypeWithThreshold;

import reactor.rx.Promise;

public class StackAndClusterUpscaleTriggerEvent extends StackScaleTriggerEvent {

    private final ScalingType scalingType;

    private final boolean singleMasterGateway;

    private final boolean kerberosSecured;

    private final boolean singleNodeCluster;

    private final boolean restartServices;

    private final ClusterManagerType clusterManagerType;

    public StackAndClusterUpscaleTriggerEvent(String selector, Long stackId, Map<String, Integer> hostGroupWithAdjustment, ScalingType scalingType,
            NetworkScaleDetails networkScaleDetails, AdjustmentTypeWithThreshold adjustmentTypeWithThreshold, String triggeredStackVariant) {
        super(selector, stackId, hostGroupWithAdjustment, Collections.emptyMap(), Collections.emptyMap(), networkScaleDetails, adjustmentTypeWithThreshold,
                triggeredStackVariant);
        this.scalingType = scalingType;
        singleMasterGateway = false;
        kerberosSecured = false;
        singleNodeCluster = false;
        restartServices = false;
        clusterManagerType = ClusterManagerType.CLOUDERA_MANAGER;
    }

    @JsonCreator
    public StackAndClusterUpscaleTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("hostGroupsWithAdjustment") Map<String, Integer> hostGroupWithAdjustment,
            @JsonProperty("hostGroupsWithPrivateIds") Map<String, Set<Long>> hostGroupWithPrivateIds,
            @JsonProperty("hostGroupsWithHostNames") Map<String, Set<String>> hostGroupWithHostNames,
            @JsonProperty("scalingType") ScalingType scalingType,
            @JsonProperty("singleMasterGateway") boolean singlePrimaryGateway,
            @JsonProperty("kerberosSecured") boolean kerberosSecured,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("singleNodeCluster") boolean singleNodeCluster,
            @JsonProperty("restartServices") boolean restartServices,
            @JsonProperty("clusterManagerType") ClusterManagerType clusterManagerType,
            @JsonProperty("adjustmentTypeWithThreshold") AdjustmentTypeWithThreshold adjustmentTypeWithThreshold,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant) {
        super(selector, stackId, hostGroupWithAdjustment, hostGroupWithPrivateIds, hostGroupWithHostNames, adjustmentTypeWithThreshold, triggeredStackVariant,
                accepted);
        this.scalingType = scalingType;
        singleMasterGateway = singlePrimaryGateway;
        this.kerberosSecured = kerberosSecured;
        this.singleNodeCluster = singleNodeCluster;
        this.restartServices = restartServices;
        this.clusterManagerType = clusterManagerType;
    }

    public ScalingType getScalingType() {
        return scalingType;
    }

    public boolean isSingleMasterGateway() {
        return singleMasterGateway;
    }

    public boolean isKerberosSecured() {
        return kerberosSecured;
    }

    public boolean isSingleNodeCluster() {
        return singleNodeCluster;
    }

    public boolean isRestartServices() {
        return restartServices;
    }

    public ClusterManagerType getClusterManagerType() {
        return clusterManagerType;
    }

    @Override
    public StackAndClusterUpscaleTriggerEvent setRepair() {
        super.setRepair();
        return this;
    }
}
