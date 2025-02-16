package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPAShape;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.InstanceGroupDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@Component
public class StackDetailsToCDPFreeIPAShapeConverter {

    private static final int DEFAULT_INTEGER_VALUE = -1;

    public CDPFreeIPAShape convert(StackDetails stackDetails) {
        CDPFreeIPAShape.Builder cdpFreeIPAShape = CDPFreeIPAShape.newBuilder();
        cdpFreeIPAShape.setNodes(DEFAULT_INTEGER_VALUE);

        if (stackDetails != null && CollectionUtils.isNotEmpty(stackDetails.getInstanceGroups())) {
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
