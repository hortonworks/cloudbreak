package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostGroupViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostMetadataViewV4Response;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceMetaDataView;

@Component
public class HostGroupViewToHostGroupViewV4ResponseConverter {

    public HostGroupViewV4Response convert(HostGroupView source, InstanceGroupView instanceGroup) {
        HostGroupViewV4Response hostGroupViewResponse = new HostGroupViewV4Response();
        hostGroupViewResponse.setId(source.getId());
        hostGroupViewResponse.setName(source.getName());
        hostGroupViewResponse.setMetadata(getHostMetadata(instanceGroup));
        return hostGroupViewResponse;
    }

    private Set<HostMetadataViewV4Response> getHostMetadata(InstanceGroupView instanceGroup) {
        Set<HostMetadataViewV4Response> hostMetadataResponses = new HashSet<>();
        Optional.ofNullable(instanceGroup)
                .map(InstanceGroupView::getNotTerminatedInstanceMetaDataSet)
                .ifPresent(instanceMetaDataSet -> {
                    for (InstanceMetaDataView instanceMetaData : instanceMetaDataSet) {
                        HostMetadataViewV4Response hostMetadataViewV4Response = new HostMetadataViewV4Response();
                        hostMetadataViewV4Response.setId(instanceMetaData.getId());
                        hostMetadataViewV4Response.setName(instanceMetaData.getInstanceName());
                        hostMetadataViewV4Response.setState(instanceMetaData.getInstanceStatus().getAsHostState());
                        hostMetadataViewV4Response.setStatusReason(instanceMetaData.getStatusReason());
                        hostMetadataResponses.add(hostMetadataViewV4Response);
                    }
                });
        return hostMetadataResponses;
    }
}
