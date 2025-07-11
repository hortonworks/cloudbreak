package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostGroupViewV4Response;
import com.sequenceiq.cloudbreak.converter.v4.blueprint.BlueprintViewToBlueprintV4ViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;

@Component
public class ClusterApiViewToClusterViewV4ResponseConverter {

    @Inject
    private DatalakeService datalakeService;

    @Inject
    private BlueprintViewToBlueprintV4ViewResponseConverter blueprintViewToBlueprintV4ViewResponseConverter;

    @Inject
    private HostGroupViewToHostGroupViewV4ResponseConverter hostGroupViewToHostGroupViewV4ResponseConverter;

    public ClusterViewV4Response convert(ClusterApiView source, Set<InstanceGroupView> instanceGroup) {
        ClusterViewV4Response clusterViewResponse = new ClusterViewV4Response();
        clusterViewResponse.setName(source.getName());
        clusterViewResponse.setDescription(source.getDescription());
        clusterViewResponse.setId(source.getId());
        clusterViewResponse.setServerIp(source.getClusterManagerIp());
        if (source.getBlueprint() != null) {
            clusterViewResponse.setBlueprint(blueprintViewToBlueprintV4ViewResponseConverter
                    .convert(source.getBlueprint()));
        }
        clusterViewResponse.setStatus(source.getStatus());
        clusterViewResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups(), instanceGroup));
        clusterViewResponse.setCertExpirationState(source.getCertExpirationState());
        datalakeService.addSharedServiceResponse(source, clusterViewResponse);
        return clusterViewResponse;
    }

    private Set<HostGroupViewV4Response> convertHostGroupsToJson(Set<HostGroupView> hostGroups, Set<InstanceGroupView> instanceGroups) {
        Map<Long, InstanceGroupView> igsById = instanceGroups.stream()
                .collect(Collectors.toMap(InstanceGroupView::getId, Function.identity()));
        Map<HostGroupView, InstanceGroupView> igByHg = hostGroups.stream()
                .filter(hg -> igsById.containsKey(hg.getInstanceGroupId()))
                .collect(Collectors.toMap(Function.identity(), hg -> igsById.get(hg.getInstanceGroupId())));
        Set<HostGroupViewV4Response> jsons = new HashSet<>();
        igByHg.forEach((hostGroupView, instanceGroupView) -> {
            jsons.add(hostGroupViewToHostGroupViewV4ResponseConverter.convert(hostGroupView, instanceGroupView));
        });

        return jsons;
    }
}
