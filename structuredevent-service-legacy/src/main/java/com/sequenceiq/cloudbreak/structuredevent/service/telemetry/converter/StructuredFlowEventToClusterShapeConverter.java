package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.event.BlueprintDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

@Component
public class StructuredFlowEventToClusterShapeConverter {

    public UsageProto.CDPClusterShape convert(StructuredFlowEvent structuredFlowEvent) {

        UsageProto.CDPClusterShape.Builder cdpClusterShape = UsageProto.CDPClusterShape.newBuilder();

        if (structuredFlowEvent != null) {
            BlueprintDetails blueprintDetails = structuredFlowEvent.getBlueprintDetails();
            if (blueprintDetails != null) {
                cdpClusterShape.setClusterTemplateName(blueprintDetails.getName());
            }

            StackDetails stackDetails = structuredFlowEvent.getStack();
            if (stackDetails != null) {
                int nodeCnt = 0;
                for (InstanceGroupDetails instanceGroupDetails : stackDetails.getInstanceGroups()) {
                    nodeCnt += instanceGroupDetails.getNodeCount();
                }
                cdpClusterShape.setNodes(nodeCnt);
                cdpClusterShape.setDefinitionDetails(JsonUtil.writeValueAsStringSilentSafe(stackDetails.getInstanceGroups()));
            }
        }
        return cdpClusterShape.build();
    }
}
