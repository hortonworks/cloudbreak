package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;

public class PublicEndpointAccessGatewayConverter extends DefaultEnumConverter<PublicEndpointAccessGateway> {
    @Override
    public PublicEndpointAccessGateway getDefault() {
        return PublicEndpointAccessGateway.DISABLED;
    }
}
