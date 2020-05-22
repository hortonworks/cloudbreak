package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameters.dao.domain.S3GuardTableCreation;

import javax.persistence.AttributeConverter;

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