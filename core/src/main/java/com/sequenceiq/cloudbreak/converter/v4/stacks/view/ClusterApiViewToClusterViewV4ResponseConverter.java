package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.AmbariViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.sharedservice.SharedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostGroupViewV4Response;
import com.sequenceiq.cloudbreak.converter.CompactViewToCompactViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.view.ClusterApiView;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class ClusterApiViewToClusterViewV4ResponseConverter extends CompactViewToCompactViewResponseConverter<ClusterApiView, ClusterViewV4Response> {

    @Inject
    private StackService stackService;

    @Override
    public ClusterViewV4Response convert(ClusterApiView source) {
        ClusterViewV4Response clusterViewResponse = super.convert(source);
        AmbariViewV4Response ambari = new AmbariViewV4Response();
        ambari.setServerIp(source.getAmbariIp());
        ambari.setBlueprint(getConversionService().convert(source.getClusterDefinition(), BlueprintV4ViewResponse.class));
        clusterViewResponse.setAmbari(ambari);
        clusterViewResponse.setStatus(source.getStatus());
        clusterViewResponse.setSecure(source.getKerberosConfig() != null);
        clusterViewResponse.setHostGroups(convertHostGroupsToJson(source.getHostGroups()));
        addSharedServiceResponse(source, clusterViewResponse);
        clusterViewResponse.setKerberosName(getKerberosName(source));
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

    private void addSharedServiceResponse(ClusterApiView cluster, ClusterViewV4Response clusterResponse) {
        SharedServiceV4Response sharedServiceResponse = new SharedServiceV4Response();
        if (cluster.getStack().getDatalakeId() != null) {
            sharedServiceResponse.setSharedClusterId(cluster.getStack().getDatalakeId());
            sharedServiceResponse.setSharedClusterName(stackService.getByIdWithTransaction(cluster.getStack().getDatalakeId()).getName());
        }
        clusterResponse.setSharedServiceResponse(sharedServiceResponse);
    }

    private String getKerberosName(ClusterApiView source) {
        return source.getKerberosConfig() != null ? source.getKerberosConfig().getName() : null;
    }

}
