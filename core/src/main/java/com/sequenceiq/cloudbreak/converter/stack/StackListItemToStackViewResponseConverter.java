package com.sequenceiq.cloudbreak.converter.stack;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialViewResponse;
import com.sequenceiq.cloudbreak.api.model.SharedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterViewResponse;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;

@Component
public class StackListItemToStackViewResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackListItemToStackViewResponseConverter.class);

    public StackViewResponse convert(StackListItem item, Map<Long, Integer> stackInstanceCounts, Map<Long, Integer> stackUnhealthyInstanceCounts,
            String sharedClusterName) {
        StackViewResponse response = new StackViewResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setCloudPlatform(item.getCloudPlatform());
        response.setCreated(item.getCreated());
        response.setStatus(item.getStackStatus());
        response.setCluster(getClusterViewResponse(item, sharedClusterName));
        response.setNodeCount(stackInstanceCounts.get(item.getId()));
        response.setUnhealthyNodeCount(stackUnhealthyInstanceCounts.get(item.getId()));
        response.setCredential(getCredentialViewResponse(item));
        return response;
    }

    private ClusterViewResponse getClusterViewResponse(StackListItem item, String sharedClusterName) {
        ClusterViewResponse clusterResponse = new ClusterViewResponse();
        clusterResponse.setBlueprint(getBlueprintViewResponse(item));
        clusterResponse.setStatus(item.getClusterStatus());
        clusterResponse.setSharedServiceResponse(getSharedServiceResponse(item, sharedClusterName));
        return clusterResponse;
    }

    private BlueprintViewResponse getBlueprintViewResponse(StackListItem item) {
        BlueprintViewResponse blueprintResponse = new BlueprintViewResponse();
        blueprintResponse.setName(item.getBlueprintName());
        blueprintResponse.setStackType(item.getStackType());
        blueprintResponse.setStackVersion(item.getStackVersion());
        Map<String, Object> tags = item.getBlueprintTags().getMap();
        blueprintResponse.setTags(tags);
        return blueprintResponse;
    }

    private SharedServiceResponse getSharedServiceResponse(StackListItem item, String sharedClusterName) {
        SharedServiceResponse sharedServiceResponse = new SharedServiceResponse();
        sharedServiceResponse.setSharedClusterId(item.getSharedClusterId());
        sharedServiceResponse.setSharedClusterName(sharedClusterName);
        return sharedServiceResponse;
    }

    private CredentialViewResponse getCredentialViewResponse(StackListItem item) {
        CredentialViewResponse credential = new CredentialViewResponse();
        credential.setCloudPlatform(item.getCloudPlatform());
        credential.setGovCloud(item.getGovCloud());
        return credential;
    }

}