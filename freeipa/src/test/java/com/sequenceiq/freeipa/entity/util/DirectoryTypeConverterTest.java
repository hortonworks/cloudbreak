package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.ldap.model.DirectoryType;

import javax.persistence.AttributeConverter;

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