package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostGroupViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostMetadataViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.HostMetadataView;

@Component
public class HostGroupViewToHostGroupViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<HostGroupView, HostGroupViewV4Response> {

    @Override
    public HostGroupViewV4Response convert(HostGroupView source) {
        HostGroupViewV4Response hostGroupViewResponse = new HostGroupViewV4Response();
        hostGroupViewResponse.setId(source.getId());
        hostGroupViewResponse.setName(source.getName());
        hostGroupViewResponse.setMetadata(getHostMetadata(source.getHostMetadata()));
        return hostGroupViewResponse;
    }

    private Set<HostMetadataViewV4Response> getHostMetadata(Iterable<HostMetadataView> hostMetadataCollection) {
        Set<HostMetadataViewV4Response> hostMetadataResponses = new HashSet<>();
        for (HostMetadataView hostMetadata : hostMetadataCollection) {
            hostMetadataResponses.add(getConversionService().convert(hostMetadata, HostMetadataViewV4Response.class));
        }
        return hostMetadataResponses;
    }
}
