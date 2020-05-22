package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;

public class IdBrokerMappingSourceConverter extends DefaultEnumConverter<IdBrokerMappingSource> {

    @Override
    public IdBrokerMappingSource getDefault() {
        return IdBrokerMappingSource.NONE;
    }
}
