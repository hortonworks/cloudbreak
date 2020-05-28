package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;

public class S3GuardTableCreationConverter extends DefaultEnumConverter<S3GuardTableCreation> {

    @Override
    public S3GuardTableCreation getDefault() {
        return S3GuardTableCreation.CREATE_NEW;
    }
}
