package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostGroupViewV4Response;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.service.sharedservice.DatalakeService;

@Component
public class ClusterApiViewToClusterViewV4ResponseConverter extends CompactViewToCompactViewResponseConverter<ClusterApiView, ClusterViewV4Response> {

    @Inject
    private DatalakeService datalakeService;

    @Override
    public ClusterViewV4Response convert(ClusterApiView source) {
        ClusterViewV4Response clusterViewResponse = super.convert(source);
        clusterViewResponse.setServerIp(source.getClusterManagerIp());
        clusterViewResponse.setBlueprint(getConversionService().convert(source.getBlueprint(), BlueprintV4ViewResponse.class));
        clusterViewResponse.setStatus(source.getStatus());
        clusterViewResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        clusterViewResponse.setCertExpirationState(source.getCertExpirationState());
        datalakeService.addSharedServiceResponse(source, clusterViewResponse);
        return clusterViewResponse;
    }

    @Override
    protected ClusterViewV4Response createTarget() {
        return new ClusterViewV4Response();
    }

    private Set<HostGroupViewV4Response> convertHostGroupsToJson(Iterable<HostGroupView> hostGroups) {
        Set<HostGroupViewV4Response> jsons = new HashSet<>();
        for (HostGroupView hostGroup : hostGroups) {
            jsons.add(getConversionService().convert(hostGroup, HostGroupViewV4Response.class));
        }
        return jsons;
    }
}
