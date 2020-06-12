package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;

public class PrivateSubnetCreationConverter extends DefaultEnumConverter<PrivateSubnetCreation> {

    @Override
    public PrivateSubnetCreation getDefault() {
        return PrivateSubnetCreation.DISABLED;
    }
}
