package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostGroupViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostMetadataViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceMetaDataView;

@Component
public class HostGroupViewToHostGroupViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<HostGroupView, HostGroupViewV4Response> {

    @Override
    public HostGroupViewV4Response convert(HostGroupView source) {
        HostGroupViewV4Response hostGroupViewResponse = new HostGroupViewV4Response();
        hostGroupViewResponse.setId(source.getId());
        hostGroupViewResponse.setName(source.getName());
        hostGroupViewResponse.setMetadata(getHostMetadata(source));
        return hostGroupViewResponse;
    }

    private Set<HostMetadataViewV4Response> getHostMetadata(HostGroupView hostGroupView) {
        Set<HostMetadataViewV4Response> hostMetadataResponses = new HashSet<>();
        Optional.ofNullable(hostGroupView.getInstanceGroup())
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
