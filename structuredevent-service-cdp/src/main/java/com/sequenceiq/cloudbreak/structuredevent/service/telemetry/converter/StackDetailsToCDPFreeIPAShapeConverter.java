package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackDetailsToCDPFreeIPAShapeConverter {

    private static final int DEFAULT_INTEGER_VALUE = -1;

    public UsageProto.CDPFreeIPAShape convert(StackDetails stackDetails) {
        UsageProto.CDPFreeIPAShape.Builder cdpFreeIPAShape = UsageProto.CDPFreeIPAShape.newBuilder();
        cdpFreeIPAShape.setNodes(DEFAULT_INTEGER_VALUE);

        if (stackDetails != null && stackDetails.getInstanceGroups() != null) {
            List<String> hostGroupNodeCount = new ArrayList<>();
            int nodeCnt = 0;
            for (InstanceGroupDetails instanceGroupDetails : stackDetails.getInstanceGroups()) {
                nodeCnt += instanceGroupDetails.getNodeCount();
                hostGroupNodeCount.add(String.format("%s=%d", instanceGroupDetails.getGroupName(), instanceGroupDetails.getNodeCount()));
            }

            cdpFreeIPAShape.setNodes(nodeCnt);
            Collections.sort(hostGroupNodeCount);
            cdpFreeIPAShape.setHostGroupNodeCount(String.join(", ", hostGroupNodeCount));
        }

        return cdpFreeIPAShape.build();
    }
}
