package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;

public class CloudStorageValidationConverter extends DefaultEnumConverter<CloudStorageValidation> {

    @Override
    public CloudStorageValidation getDefault() {
        return CloudStorageValidation.DISABLED;
    }
}
