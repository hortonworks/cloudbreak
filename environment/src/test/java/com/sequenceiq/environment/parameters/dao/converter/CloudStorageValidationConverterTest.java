package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;

public class CloudStorageValidationConverterTest extends DefaultEnumConverterBaseTest<CloudStorageValidation> {

    @Override
    public CloudStorageValidation getDefaultValue() {
        return CloudStorageValidation.DISABLED;
    }

    @Override
    public AttributeConverter<CloudStorageValidation, String> getVictim() {
        return new CloudStorageValidationConverter();
    }
}
