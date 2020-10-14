package com.sequenceiq.environment.network.dao.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;

public class ServiceEndpointCreationConverter extends DefaultEnumConverter<ServiceEndpointCreation> {
    @Override
    public ServiceEndpointCreation getDefault() {
        return ServiceEndpointCreation.DISABLED;
    }
}
