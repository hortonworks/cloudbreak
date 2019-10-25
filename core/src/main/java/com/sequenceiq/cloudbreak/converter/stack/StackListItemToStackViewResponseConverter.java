package com.sequenceiq.cloudbreak.converter.stack;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.BlueprintViewResponse;
import com.sequenceiq.cloudbreak.api.model.CredentialViewResponse;
import com.sequenceiq.cloudbreak.api.model.SharedServiceResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.HostGroupViewResponse;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;

@Component
public class StackListItemToStackViewResponseConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackListItemToStackViewResponseConverter.class);

    public StackViewResponse convert(StackListItem item, Map<Long, Integer> stackInstanceCounts, Map<Long, Integer> stackUnhealthyInstanceCounts,
            String sharedClusterName, List<HostGroupView> hostGroupViews) {
        StackViewResponse response = new StackViewResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setCloudPlatform(item.getCloudPlatform());
        response.setPlatformVariant(item.getPlatformVariant());
        response.setCreated(item.getCreated());
        response.setStatus(item.getStackStatus());
        response.setCluster(getClusterViewResponse(item, sharedClusterName, hostGroupViews));
        response.setNodeCount(stackInstanceCounts.get(item.getId()));
        response.setUnhealthyNodeCount(stackUnhealthyInstanceCounts.get(item.getId()));
        response.setCredential(getCredentialViewResponse(item));
        response.setTerminated(item.getTerminated());
        return response;
    }

    private ClusterViewResponse getClusterViewResponse(StackListItem item, String sharedClusterName, List<HostGroupView> hostGroupViews) {
        ClusterViewResponse clusterResponse = new ClusterViewResponse();
        clusterResponse.setId(item.getClusterId());
        clusterResponse.setName(item.getName());
        clusterResponse.setSecure(item.getSecure());
        clusterResponse.setAmbariServerIp(item.getAmbariIp());
        clusterResponse.setBlueprint(getBlueprintViewResponse(item));
        clusterResponse.setStatus(item.getClusterStatus());
        clusterResponse.setSharedServiceResponse(getSharedServiceResponse(item, sharedClusterName));
        clusterResponse.setHostGroups(getHostGroupViewResponse(hostGroupViews));
        return clusterResponse;
    }

    private Set<HostGroupViewResponse> getHostGroupViewResponse(List<HostGroupView> hostGroupViews) {
        return hostGroupViews.stream().map(hgView -> {
            HostGroupViewResponse hgResponse = new HostGroupViewResponse();
            hgResponse.setId(hgView.getId());
            hgResponse.setName(hgView.getName());
            return hgResponse;
        }).collect(Collectors.toSet());
    }

    private BlueprintViewResponse getBlueprintViewResponse(StackListItem item) {
        BlueprintViewResponse blueprintResponse = new BlueprintViewResponse();
        blueprintResponse.setId(item.getBlueprintId());
        blueprintResponse.setName(item.getBlueprintName());
        blueprintResponse.setStackType(item.getStackType());
        blueprintResponse.setStackVersion(item.getStackVersion());
        blueprintResponse.setHostGroupCount(item.getHostGroupCount());
        blueprintResponse.setStatus(item.getBlueprintStatus());
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
        credential.setName(item.getCredentialName());
        credential.setCloudPlatform(item.getCloudPlatform());
        credential.setGovCloud(item.getGovCloud());
        return credential;
    }

}