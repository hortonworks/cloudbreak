package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

@Component
public class StructuredEventToClusterShapeConverter {

    private static final int DEFAULT_INTEGER_VALUE = -1;

    public UsageProto.CDPClusterShape convert(StructuredFlowEvent structuredFlowEvent) {
        UsageProto.CDPClusterShape.Builder cdpClusterShape = UsageProto.CDPClusterShape.newBuilder();

        cdpClusterShape.setNodes(DEFAULT_INTEGER_VALUE);

        if (structuredFlowEvent != null) {
            cdpClusterShape = convert(structuredFlowEvent.getBlueprintDetails(), structuredFlowEvent.getStack());
        }

        return cdpClusterShape.build();
    }

    public UsageProto.CDPClusterShape convert(StructuredSyncEvent structuredSyncEvent) {
        UsageProto.CDPClusterShape.Builder cdpClusterShape = UsageProto.CDPClusterShape.newBuilder();
        cdpClusterShape.setNodes(DEFAULT_INTEGER_VALUE);

        if (structuredSyncEvent != null) {
            cdpClusterShape = convert(structuredSyncEvent.getBlueprintDetails(), structuredSyncEvent.getStack());
        }

        return cdpClusterShape.build();
    }

    private UsageProto.CDPClusterShape.Builder convert(BlueprintDetails blueprintDetails, StackDetails stackDetails) {
        UsageProto.CDPClusterShape.Builder cdpClusterShape = UsageProto.CDPClusterShape.newBuilder();
        cdpClusterShape.setNodes(DEFAULT_INTEGER_VALUE);

        if (blueprintDetails != null) {
            cdpClusterShape.setClusterTemplateName(defaultIfEmpty(blueprintDetails.getName(), ""));
        }

        if (stackDetails != null) {
            List<String> hostGroupNodeCount = new ArrayList<>();
            Set<String> hostGroupTemporaryStorage = new HashSet<>();
            int nodeCnt = 0;
            for (InstanceGroupDetails instanceGroupDetails : stackDetails.getInstanceGroups()) {
                nodeCnt += instanceGroupDetails.getNodeCount();
                hostGroupNodeCount.add(String.format("%s=%d", instanceGroupDetails.getGroupName(), instanceGroupDetails.getNodeCount()));
                hostGroupTemporaryStorage.add(instanceGroupDetails.getTemporaryStorage());
            }

            cdpClusterShape.setNodes(nodeCnt);
            Collections.sort(hostGroupNodeCount);
            cdpClusterShape.setHostGroupNodeCount(String.join(", ", hostGroupNodeCount));
            cdpClusterShape.setTemporaryStorageUsed(hostGroupTemporaryStorage.contains(TemporaryStorage.EPHEMERAL_VOLUMES.name()));
            cdpClusterShape.setDefinitionDetails(JsonUtil.writeValueAsStringSilentSafe(stackDetails.getInstanceGroups()));
        }

        return cdpClusterShape;
    }
}
