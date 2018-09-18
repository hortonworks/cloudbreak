package com.sequenceiq.cloudbreak.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.HostMetadataViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.HostMetadataView;

@Component
public class HostMetadataViewToHostMetadataViewResponseConverter extends AbstractConversionServiceAwareConverter<HostMetadataView, HostMetadataViewResponse> {
    @Override
    public HostMetadataViewResponse convert(HostMetadataView source) {
        HostMetadataViewResponse hostMetadataViewResponse = new HostMetadataViewResponse();
        hostMetadataViewResponse.setId(source.getId());
        hostMetadataViewResponse.setName(source.getHostName());
        hostMetadataViewResponse.setState(source.getHostMetadataState().name());
        return hostMetadataViewResponse;
    }
}
