package com.sequenceiq.cloudbreak.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.common.model.FileSystemType;

public class FileSystemTypeConverterTest extends DefaultEnumConverterBaseTest<FileSystemType> {

    @Override
    public FileSystemType getDefaultValue() {
        return FileSystemType.S3;
    }

    @Override
    public AttributeConverter<FileSystemType, String> getVictim() {
        return new FileSystemTypeConverter();
    }
}
