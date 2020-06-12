package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;

import javax.persistence.AttributeConverter;

public class PrivateSubnetCreationConverterTest extends DefaultEnumConverterBaseTest<PrivateSubnetCreation> {

    @Override
    public PrivateSubnetCreation getDefaultValue() {
        return PrivateSubnetCreation.DISABLED;
    }

    @Override
    public AttributeConverter<PrivateSubnetCreation, String> getVictim() {
        return new PrivateSubnetCreationConverter();
    }
}