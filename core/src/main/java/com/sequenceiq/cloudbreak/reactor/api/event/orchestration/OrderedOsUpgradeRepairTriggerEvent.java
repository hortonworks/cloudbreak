package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import static com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers.CLUSTER_REPAIR_TRIGGER_EVENT;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The persisted event type prevents builds without deferred disk retention from resuming this repair.
 */
public class OrderedOsUpgradeRepairTriggerEvent extends ClusterRepairTriggerEvent {

    @JsonCreator
    public OrderedOsUpgradeRepairTriggerEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("failedNodesMap") Map<String, List<String>> failedNodesMap,
            @JsonProperty("triggeredStackVariant") String triggeredStackVariant,
            @JsonProperty("instancesToRetainVolumes") Set<String> instancesToRetainVolumes) {
        super(CLUSTER_REPAIR_TRIGGER_EVENT, stackId, RepairType.ALL_AT_ONCE, failedNodesMap, false, triggeredStackVariant, true);
        setInstancesToRetainVolumes(instancesToRetainVolumes);
    }
}
