package com.sequenceiq.cloudbreak.converter.stack.cluster.host;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;

@Component
public class HostMetadataToHostMetadataResponseConverter extends AbstractConversionServiceAwareConverter<HostMetadata, HostMetadataResponse> {

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
