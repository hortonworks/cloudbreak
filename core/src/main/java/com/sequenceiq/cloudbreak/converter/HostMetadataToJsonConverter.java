package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.HostMetadataResponse;
import com.sequenceiq.cloudbreak.domain.HostMetadata;

@Component
public class HostMetadataToJsonConverter extends AbstractConversionServiceAwareConverter<HostMetadata, HostMetadataResponse> {

    @Override
    public HostMetadataResponse convert(HostMetadata source) {
        HostMetadataResponse hostMetadataResponse = new HostMetadataResponse();
        hostMetadataResponse.setId(source.getId());
        hostMetadataResponse.setGroupName(source.getHostGroup().getName());
        hostMetadataResponse.setName(source.getHostName());
        hostMetadataResponse.setState(source.getHostMetadataState().name());
        return hostMetadataResponse;
    }
}
