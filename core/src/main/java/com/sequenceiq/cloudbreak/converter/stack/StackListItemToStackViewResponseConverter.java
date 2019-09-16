package com.sequenceiq.cloudbreak.converter.stack;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterViewResponse;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;

@Component
public class StackListItemToStackViewResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackListItemToStackViewResponseConverter.class);

    public StackViewResponse convert(StackListItem item, Map<Long, Integer> stackInstanceCounts, Map<Long, Integer> stackUnhealthyInstanceCounts) {
        StackViewResponse response = new StackViewResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setCloudPlatform(item.getCloudPlatform());
        response.setCreated(item.getCreated());
        response.setStatus(item.getStackStatus());
        ClusterViewResponse clusterResponse = new ClusterViewResponse();
        BlueprintViewResponse blueprintResponse = new BlueprintViewResponse();
        blueprintResponse.setName(item.getBlueprintName());
        blueprintResponse.setStackType(item.getStackType());
        blueprintResponse.setStackVersion(item.getStackVersion());
        clusterResponse.setBlueprint(blueprintResponse);
        clusterResponse.setStatus(item.getClusterStatus());
        response.setCluster(clusterResponse);
        response.setNodeCount(stackInstanceCounts.get(item.getId()));
        response.setUnhealthyNodeCount(stackUnhealthyInstanceCounts.get(item.getId()));
        return response;
    }

}