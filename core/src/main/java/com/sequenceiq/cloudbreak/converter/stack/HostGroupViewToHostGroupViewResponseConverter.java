package com.sequenceiq.cloudbreak.converter.stack;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.HostGroupViewResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.HostMetadataViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.HostMetadataView;

@Component
public class HostGroupViewToHostGroupViewResponseConverter extends AbstractConversionServiceAwareConverter<HostGroupView, HostGroupViewResponse> {

    @Override
    public HostGroupViewResponse convert(HostGroupView source) {
        HostGroupViewResponse hostGroupViewResponse = new HostGroupViewResponse();
        hostGroupViewResponse.setId(source.getId());
        hostGroupViewResponse.setName(source.getName());
        hostGroupViewResponse.setMetadata(getHostMetadata(source.getHostMetadata()));
        return hostGroupViewResponse;
    }

    private Set<HostMetadataViewResponse> getHostMetadata(Iterable<HostMetadataView> hostMetadataCollection) {
        Set<HostMetadataViewResponse> hostMetadataResponses = new HashSet<>();
        for (HostMetadataView hostMetadata : hostMetadataCollection) {
            hostMetadataResponses.add(getConversionService().convert(hostMetadata, HostMetadataViewResponse.class));
        }
        return hostMetadataResponses;
    }
}
