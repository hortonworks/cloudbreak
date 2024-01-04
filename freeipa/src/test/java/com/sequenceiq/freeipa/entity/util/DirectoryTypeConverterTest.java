package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;

public class DirectoryTypeConverterTest extends DefaultEnumConverterBaseTest<DirectoryType> {

    @Override
    public DirectoryType getDefaultValue() {
        return DirectoryType.ACTIVE_DIRECTORY;
    }

    @Override
    public AttributeConverter<DirectoryType, String> getVictim() {
        return new DirectoryTypeConverter();
    }
}
