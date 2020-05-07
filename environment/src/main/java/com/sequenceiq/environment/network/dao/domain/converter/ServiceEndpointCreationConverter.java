package com.sequenceiq.environment.network.dao.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.ServiceEndpointCreation;

public class ServiceEndpointCreationConverter extends DefaultEnumConverter<ServiceEndpointCreation> {
    @Override
    public ServiceEndpointCreation getDefault() {
        return ServiceEndpointCreation.DISABLED;
    }
}
