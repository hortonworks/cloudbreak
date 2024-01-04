package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;

public class IdBrokerMappingSourceConverterTest extends DefaultEnumConverterBaseTest<IdBrokerMappingSource> {

    @Override
    public IdBrokerMappingSource getDefaultValue() {
        return IdBrokerMappingSource.NONE;
    }

    @Override
    public AttributeConverter<IdBrokerMappingSource, String> getVictim() {
        return new IdBrokerMappingSourceConverter();
    }
}
