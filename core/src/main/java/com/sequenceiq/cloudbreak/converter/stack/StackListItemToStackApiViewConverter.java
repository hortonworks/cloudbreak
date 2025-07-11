package com.sequenceiq.cloudbreak.converter.stack;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.view.BlueprintView;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.domain.view.StackStatusView;
import com.sequenceiq.cloudbreak.domain.view.UserView;

@Component
public class StackListItemToStackApiViewConverter {

    public StackApiView convert(StackListItem item, Map<Long, Integer> stackInstanceCounts,
            Collection<HostGroupView> hostGroupViews, Collection<InstanceGroupView> instanceGroupViews) {
        StackApiView response = new StackApiView();
        response.setId(item.getId());
        response.setResourceCrn(item.getResourceCrn());
        response.setName(item.getName());
        response.setCloudPlatform(item.getCloudPlatform());
        response.setPlatformVariant(item.getPlatformVariant());
        response.setCreated(item.getCreated());
        StackStatusView stackStatusView = new StackStatusView();
        stackStatusView.setStatus(item.getStackStatus());
        response.setStackStatus(stackStatusView);
        response.setCluster(getClusterApiView(item, hostGroupViews));
        response.setInstanceGroups(instanceGroupViews == null ? Collections.emptySet() : new HashSet<>(instanceGroupViews));
        response.setNodeCount(stackInstanceCounts.get(item.getId()));
        response.setTunnel(item.getTunnel());
        response.setEnvironmentCrn(item.getEnvironmentCrn());
        response.setType(item.getType());
        response.setDatalakeCrn(item.getDatalakeCrn());
        response.setUserView(getUserView(item));
        response.setTerminated(item.getTerminated());
        response.setStackVersion(item.getStackVersion());
        response.getCluster().setStack(response);
        response.setExternalDatabaseCreationType(item.getExternalDatabaseCreationType());
        response.setExternalDatabaseEngineVersion(item.getExternalDatabaseEngineVersion());
        response.setProviderSyncStates(item.getProviderSyncStates());
        return response;
    }

    private UserView getUserView(StackListItem item) {
        UserView userView = new UserView();
        userView.setId(item.getUserDOId());
        userView.setUserId(item.getUserId());
        userView.setUserName(item.getUsername());
        userView.setUserCrn(item.getUsercrn());
        return userView;
    }

    private ClusterApiView getClusterApiView(StackListItem item, Collection<HostGroupView> hostGroupViews) {
        ClusterApiView clusterResponse = new ClusterApiView();
        clusterResponse.setId(item.getClusterId());
        clusterResponse.setEnvironmentCrn(item.getEnvironmentCrn());
        clusterResponse.setName(item.getName());
        clusterResponse.setClusterManagerIp(item.getClusterManagerIp());
        clusterResponse.setBlueprint(getBlueprintView(item));
        clusterResponse.setStatus(item.getClusterStatus());
        clusterResponse.setHostGroups(hostGroupViews == null ? Collections.emptySet() : new HashSet<>(hostGroupViews));
        clusterResponse.setCertExpirationState(item.getCertExpirationState());
        return clusterResponse;
    }

    private BlueprintView getBlueprintView(StackListItem item) {
        BlueprintView blueprintResponse = new BlueprintView();
        blueprintResponse.setId(item.getBlueprintId());
        blueprintResponse.setResourceCrn(item.getBlueprintCrn());
        blueprintResponse.setCreated(item.getBlueprintCreated());
        blueprintResponse.setName(item.getBlueprintName());
        blueprintResponse.setStackType(item.getStackType());
        blueprintResponse.setStackVersion(item.getStackVersion());
        blueprintResponse.setHostGroupCount(item.getHostGroupCount() == null ? 0 : item.getHostGroupCount());
        blueprintResponse.setStatus(item.getBlueprintStatus());
        blueprintResponse.setTags(item.getBlueprintTags());
        blueprintResponse.setBlueprintUpgradeOption(item.getBlueprintUpgradeOption());
        blueprintResponse.setLastUpdated(item.getLastUpdated());
        return blueprintResponse;
    }
}