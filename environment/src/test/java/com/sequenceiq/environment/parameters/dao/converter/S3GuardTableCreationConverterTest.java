package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameter.dto.s3guard.S3GuardTableCreation;

public class S3GuardTableCreationConverterTest extends DefaultEnumConverterBaseTest<S3GuardTableCreation> {

    @Override
    public S3GuardTableCreation getDefaultValue() {
        return S3GuardTableCreation.CREATE_NEW;
    }

    @Override
    public AttributeConverter<S3GuardTableCreation, String> getVictim() {
        return new S3GuardTableCreationConverter();
    }
}
