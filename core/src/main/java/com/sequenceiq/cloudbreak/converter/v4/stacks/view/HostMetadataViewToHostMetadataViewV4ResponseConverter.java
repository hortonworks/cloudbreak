package com.sequenceiq.cloudbreak.converter.v4.stacks.view;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.HostMetadataViewV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.HostMetadataView;

@Component
public class HostMetadataViewToHostMetadataViewV4ResponseConverter extends AbstractConversionServiceAwareConverter<HostMetadataView,
        HostMetadataViewV4Response> {

    @Override
    public HostMetadataViewV4Response convert(HostMetadataView source) {
        HostMetadataViewV4Response hostMetadataViewResponse = new HostMetadataViewV4Response();
        hostMetadataViewResponse.setId(source.getId());
        hostMetadataViewResponse.setName(source.getHostName());
        hostMetadataViewResponse.setState(source.getHostMetadataState().name());
        hostMetadataViewResponse.setStatusReason(source.getStatusReason());
        return hostMetadataViewResponse;
    }

}
